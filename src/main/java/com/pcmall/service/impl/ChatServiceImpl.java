package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.client.DeepSeekTimeoutException;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.ChatRequest;
import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.ChatMessage;
import com.pcmall.entity.ChatSession;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ChatMessageMapper;
import com.pcmall.mapper.ChatSessionMapper;
import com.pcmall.service.ChatService;
import com.pcmall.service.ProductService;
import com.pcmall.service.ProductSearchTool;
import com.pcmall.vo.ChatResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {
    private static final int HISTORY_LIMIT = 12;
    private static final int MAX_TOOL_CALLS = 3;
    private static final String SYSTEM_PROMPT = "你是电脑导购助手。只理解用户需求并给出推荐理由。产品名称、价格和参数只能引用 search_products 工具返回的 MySQL 数据。";
    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final DeepSeekClient deepSeekClient;
    private final ProductSearchTool productSearchTool;
    private final ProductService productService;
    private final ObjectMapper objectMapper;

    public ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, DeepSeekClient deepSeekClient,
                           ProductSearchTool productSearchTool, ProductService productService, ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.deepSeekClient = deepSeekClient;
        this.productSearchTool = productSearchTool;
        this.productService = productService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        validate(request);
        String systemPrompt = systemPrompt(request.contextProductId());
        ensureSession(request.sessionId());
        List<ObjectNode> messages = loadHistory(request.sessionId(), systemPrompt);
        ObjectNode userMessage = message("user", request.message());
        messages.add(userMessage);
        saveMessage(request.sessionId(), "USER", request.message());
        try {
            DeepSeekCompletion completion = deepSeekClient.complete(messages);
            Map<Long, Product> productsById = new LinkedHashMap<>();
            for (int toolCallCount = 0; completion.hasToolCall() && toolCallCount < MAX_TOOL_CALLS; toolCallCount++) {
                List<Product> searchedProducts = productSearchTool.search(parseCriteria(completion.toolArguments()));
                searchedProducts.forEach(product -> productsById.putIfAbsent(product.getId(), product));
                messages.add(toolCallMessage(completion));
                messages.add(toolResultMessage(completion.toolCallId(), searchedProducts));
                completion = deepSeekClient.complete(messages);
            }
            String reply = completion.content();
            if (completion.hasToolCall() || reply == null || reply.isBlank()) throw new BusinessException(HttpStatus.BAD_GATEWAY, "DeepSeek 返回格式不合法");
            saveMessage(request.sessionId(), "ASSISTANT", reply);
            return new ChatResult(reply, List.copyOf(productsById.values()));
        } catch (DeepSeekTimeoutException e) {
            List<Product> fallbackProducts = productSearchTool.search(new ProductSearchCriteria(null, null, List.of()));
            String fallbackReply = "当前推荐服务响应较慢，先为你展示热门商品。";
            saveMessage(request.sessionId(), "ASSISTANT", fallbackReply);
            return new ChatResult(fallbackReply, fallbackProducts);
        }
    }

    private void validate(ChatRequest request) {
        if (request == null || request.sessionId() == null || request.message() == null || request.message().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "sessionId 和 message 不能为空");
        }
        try { UUID.fromString(request.sessionId()); }
        catch (IllegalArgumentException e) { throw new BusinessException(HttpStatus.BAD_REQUEST, "sessionId 必须是 UUID"); }
        if (request.message().length() > 500) throw new BusinessException(HttpStatus.BAD_REQUEST, "message 不能超过 500 个字符");
    }

    private void ensureSession(String sessionId) {
        if (sessionMapper.selectById(sessionId) != null) return;
        ChatSession session = new ChatSession();
        session.setId(sessionId);
        sessionMapper.insert(session);
    }

    private List<ObjectNode> loadHistory(String sessionId, String systemPrompt) {
        List<ChatMessage> history = new ArrayList<>(messageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .eq(ChatMessage::getSessionId, sessionId).orderByDesc(ChatMessage::getCreatedAt).last("LIMIT " + HISTORY_LIMIT)));
        Collections.reverse(history);
        List<ObjectNode> messages = new ArrayList<>();
        messages.add(message("system", systemPrompt));
        history.forEach(item -> messages.add(message(item.getRole().toLowerCase(), item.getContent())));
        return messages;
    }

    private String systemPrompt(Long contextProductId) {
        if (contextProductId == null) return SYSTEM_PROMPT;
        Product product = productService.getActiveById(contextProductId);
        return SYSTEM_PROMPT + "\n[当前用户正在查看商品: " + product.getName()
                + ", 价格: " + product.getPrice() + "元, CPU: " + product.getCpu()
                + ", GPU: " + product.getGpu() + ", 内存: " + product.getMemoryGb()
                + "GB, 存储: " + product.getStorageGb() + "GB]";
    }

    private ProductSearchCriteria parseCriteria(String arguments) {
        try {
            var root = objectMapper.readTree(arguments);
            List<String> tags = new ArrayList<>();
            root.path("tags").forEach(tag -> tags.add(tag.asText()));
            return new ProductSearchCriteria(root.hasNonNull("maxBudget") ? root.get("maxBudget").decimalValue() : null,
                    root.path("category").asText(null), tags);
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "DeepSeek 工具参数不合法");
        }
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private ObjectNode toolCallMessage(DeepSeekCompletion completion) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "assistant");
        ArrayNode calls = message.putArray("tool_calls");
        ObjectNode call = calls.addObject();
        call.put("id", completion.toolCallId());
        call.put("type", "function");
        call.putObject("function").put("name", completion.toolName()).put("arguments", completion.toolArguments());
        return message;
    }

    private ObjectNode toolResultMessage(String callId, List<Product> products) {
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "tool");
        message.put("tool_call_id", callId);
        try { message.put("content", objectMapper.writeValueAsString(products)); }
        catch (JsonProcessingException e) { throw new IllegalStateException("产品序列化失败", e); }
        return message;
    }

    private void saveMessage(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }
}

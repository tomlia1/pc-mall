package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.client.DeepSeekTimeoutException;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.ChatRequest;
import com.pcmall.entity.ChatMessage;
import com.pcmall.entity.ChatSession;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ChatMessageMapper;
import com.pcmall.mapper.ChatSessionMapper;
import com.pcmall.service.BudgetFeasibilityService;
import com.pcmall.service.ChatService;
import com.pcmall.service.ProductSearchTool;
import com.pcmall.service.ProductService;
import com.pcmall.vo.BudgetFeasibilityResult;
import com.pcmall.vo.ChatResult;
import com.pcmall.vo.ProductMatchType;
import com.pcmall.vo.ProductSearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ChatServiceImpl implements ChatService {
    private static final int HISTORY_LIMIT = 12;
    private static final Pattern UNSUPPORTED_BUDGET_CLAIM = Pattern.compile("(?i)(兼容.{0,8}通过|检查通过|完全兼容|(适合|满足|可以|可用).{0,12}(吃鸡|游戏|3A))");
    private static final Pattern PERFORMANCE_PROMISE = Pattern.compile("(?i)(稳定.{0,8}(吃鸡|游戏)|流畅.{0,8}(吃鸡|游戏)|帧率|fps|能跑多少|性能保证)");
    private static final Pattern ALTERNATIVE_AS_EXACT = Pattern.compile("(完全符合|精准匹配|非常适合|完美符合)");
    private static final Pattern EXACT_CONTRADICTION = Pattern.compile("(没有找到|没有完全匹配|不符合|无匹配)");
    private static final String GENERAL_CLARIFY_REPLY = "请补充预算和用途，并说明更偏向笔记本、DIY 装机，或具体想咨询哪类配件。";
    private static final String PART_CLARIFY_REPLY = "你想咨询哪类配件？请说明是 CPU、显卡、主板、内存、SSD、电源还是机箱。";
    private static final String NONE_REPLY = "当前商品库没有符合已确定条件的在售商品。你可以调整预算、用途或商品类型后再试。";
    private static final String AI_UNAVAILABLE_REPLY = "AI 导购暂时不可用，请稍后重试。";
    private static final String SYSTEM_PROMPT = "你是电脑导购助手。只理解用户需求并解释后端给出的商品事实。最新一条用户需求优先；普通文本输出，不得使用 Markdown、#、**、---、表格或 Emoji 标题。商品名称、价格、配置和匹配类型只以服务端提供的商品库结果为准，不得编造商品、价格或匹配结论。";

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final DeepSeekClient deepSeekClient;
    private final ProductSearchTool productSearchTool;
    private final ProductService productService;
    private final ObjectMapper objectMapper;
    private final BudgetFeasibilityService budgetFeasibilityService;
    private final ChatIntentRouter intentRouter = new ChatIntentRouter();

    @Autowired
    public ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, DeepSeekClient deepSeekClient,
                           ProductSearchTool productSearchTool, ProductService productService, ObjectMapper objectMapper,
                           BudgetFeasibilityService budgetFeasibilityService) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
        this.deepSeekClient = deepSeekClient;
        this.productSearchTool = productSearchTool;
        this.productService = productService;
        this.objectMapper = objectMapper;
        this.budgetFeasibilityService = budgetFeasibilityService;
    }

    ChatServiceImpl(ChatSessionMapper sessionMapper, ChatMessageMapper messageMapper, DeepSeekClient deepSeekClient,
                    ProductSearchTool productSearchTool, ProductService productService, ObjectMapper objectMapper) {
        this(sessionMapper, messageMapper, deepSeekClient, productSearchTool, productService, objectMapper, null);
    }

    @Override
    public ChatResult chat(ChatRequest request) {
        validate(request);
        String basePrompt = systemPrompt(request.contextProductId());
        ensureSession(request.sessionId());
        List<ObjectNode> messages = loadHistory(request.sessionId(), basePrompt);
        List<String> previousUserMessages = messages.stream()
                .filter(item -> "user".equals(item.path("role").asText()))
                .map(item -> item.path("content").asText()).toList();
        ChatIntentRouter.RoutedIntent intent = intentRouter.route(request.message(), previousUserMessages);
        messages.add(message("user", request.message()));
        saveMessage(request.sessionId(), "USER", request.message());

        if (request.contextProductId() != null && isCurrentProductQuestion(request.message())) {
            return generalChat(request.sessionId(), messages);
        }

        return switch (intent.route()) {
            case CLARIFY -> fixedReply(request.sessionId(), PART_CLARIFY_REPLY);
            case BUDGET_CLARIFY -> fixedReply(request.sessionId(), "已收到你的预算。请再告诉我用途，以及更想看笔记本、完整 DIY 主机，还是某一类配件，我会只按商品库中的真实在售商品查询。");
            case BUILD_PLAN -> buildPlanChat(request.sessionId(), messages, intent.criteria().maxBudget());
            case LAPTOP_SEARCH, PART_SEARCH -> productSearchChat(request.sessionId(), messages, intent.criteria());
            case GENERAL -> generalChat(request.sessionId(), messages);
        };
    }

    private ChatResult buildPlanChat(String sessionId, List<ObjectNode> messages, BigDecimal budget) {
        if (budget == null || budgetFeasibilityService == null) return fixedReply(sessionId, GENERAL_CLARIFY_REPLY);
        BudgetFeasibilityResult result = budgetFeasibilityService.findForBudget(budget).orElse(null);
        if (result == null) return fixedReply(sessionId, "当前商品库暂时无法计算完整独显方案，请稍后重试。\n");
        messages.set(0, message("system", messages.get(0).path("content").asText() + budgetPrompt(result)));
        try {
            String reply = sanitizeBudgetReply(requireText(deepSeekClient.completePlain(messages)));
            return reply.isBlank() ? budgetFallbackResult(sessionId, result) : saveAndReturn(sessionId, reply, List.of(), result, null);
        } catch (DeepSeekTimeoutException | BusinessException exception) {
            return budgetFallbackResult(sessionId, result);
        }
    }

    private ChatResult productSearchChat(String sessionId, List<ObjectNode> messages, com.pcmall.dto.ProductSearchCriteria criteria) {
        ProductSearchResult searchResult = productSearchTool.search(criteria);
        if (searchResult.matchType() == ProductMatchType.NONE) {
            return saveAndReturn(sessionId, NONE_REPLY, List.of(), null, searchResult);
        }
        if (searchResult.matchType() == ProductMatchType.ALTERNATIVE) {
            return saveAndReturn(sessionId, fallbackSearchReply(searchResult), searchResult.products(), null, searchResult);
        }
        messages.set(0, message("system", messages.get(0).path("content").asText() + productSearchPrompt(searchResult)));
        try {
            String reply = sanitizeSearchReply(requireText(deepSeekClient.completePlain(messages)), searchResult);
            if (reply.isBlank()) reply = fallbackSearchReply(searchResult);
            return saveAndReturn(sessionId, reply, searchResult.products(), null, searchResult);
        } catch (DeepSeekTimeoutException | BusinessException exception) {
            return saveAndReturn(sessionId, AI_UNAVAILABLE_REPLY + " 已保留本次商品库检索结果。",
                    searchResult.products(), null, searchResult);
        }
    }

    private ChatResult generalChat(String sessionId, List<ObjectNode> messages) {
        try {
            String reply = requireText(deepSeekClient.completePlain(messages));
            return reply.isBlank() ? fixedReply(sessionId, GENERAL_CLARIFY_REPLY) : saveAndReturn(sessionId, reply, List.of(), null, null);
        } catch (DeepSeekTimeoutException | BusinessException exception) {
            return fixedReply(sessionId, AI_UNAVAILABLE_REPLY);
        }
    }

    private String requireText(DeepSeekCompletion completion) {
        return completion == null || completion.hasToolCall() || completion.content() == null ? "" : completion.content().trim();
    }

    private ChatResult budgetFallbackResult(String sessionId, BudgetFeasibilityResult result) {
        String reply = result.hasWithinBudgetPlan()
                ? "预算结论已生成，当前预算内有一套后端计算的最低价基础兼容独显方案，可由你点击后填入装机单。AI 解读暂时不可用，请稍后重试。"
                : "当前预算下，商品库没有预算内的完整独显方案。AI 解读暂时不可用，请稍后重试。";
        return saveAndReturn(sessionId, reply, List.of(), result, null);
    }

    private String fallbackSearchReply(ProductSearchResult result) {
        return result.matchType() == ProductMatchType.ALTERNATIVE
                ? "以下是保留商品类型、预算和用途后的相关备选，供你继续比较。"
                : "以下商品符合已确定的商品类型、预算和用途条件。";
    }

    private String sanitizeSearchReply(String reply, ProductSearchResult result) {
        String cleaned = plainText(reply);
        if (result.matchType() == ProductMatchType.ALTERNATIVE) {
            cleaned = ALTERNATIVE_AS_EXACT.matcher(cleaned).replaceAll("可作为参考");
            return "以下为相关备选，不代表完全符合全部偏好。\n" + cleaned;
        }
        return cleaned.lines()
                .flatMap(line -> Arrays.stream(line.split("(?<=[。！？])\\s*")))
                .filter(line -> !EXACT_CONTRADICTION.matcher(line).find())
                .map(String::trim).filter(line -> !line.isBlank())
                .reduce((left, right) -> left + "\n" + right).orElse("");
    }

    static String sanitizeBudgetReply(String reply) {
        return plainText(reply).lines()
                .flatMap(line -> Arrays.stream(line.split("(?<=[。！？])\\s*")))
                .filter(line -> !PERFORMANCE_PROMISE.matcher(line).find())
                .filter(line -> !UNSUPPORTED_BUDGET_CLAIM.matcher(line).find())
                .filter(line -> !line.matches("^\\s*\\d+[.、].*"))
                .map(String::trim).filter(line -> !line.isBlank()).reduce((left, right) -> left + "\n" + right).orElse("").trim();
    }

    private static String plainText(String text) {
        return text == null ? "" : text.replaceAll("(?m)^\\s*(#{1,6}\\s*|\\*{1,2}|---\\s*)", "").trim();
    }

    private String productSearchPrompt(ProductSearchResult result) {
        try {
            return "\n[服务端商品检索结果，必须遵守] matchType=" + result.matchType()
                    + "；reason=" + result.reason() + "；products=" + objectMapper.writeValueAsString(result.products())
                    + "。EXACT 才能说符合条件；ALTERNATIVE 只能称为相关备选；不得自行查询或调用任何工具。";
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("商品检索结果序列化失败", exception);
        }
    }

    private String budgetPrompt(BudgetFeasibilityResult result) {
        String conclusion = "\n[后端预算结论，必须遵守] 当前预算: " + money(result.budget())
                + "元；当前库最低基础兼容独显方案总价: " + money(result.minimumFeasibleTotal())
                + "元；预算差额: " + money(result.budgetGap()) + "元；预算内是否存在方案: " + result.hasWithinBudgetPlan();
        return conclusion + (result.hasWithinBudgetPlan()
                ? "。只能说明用户可点击一键填入装机单，不得逐条列零件或承诺游戏性能。"
                : "。明确当前库没有预算内完整独显方案；不要列零件、不要承诺游戏性能。");
    }

    private ChatResult fixedReply(String sessionId, String reply) {
        return saveAndReturn(sessionId, reply, List.of(), null, null);
    }

    private boolean isCurrentProductQuestion(String message) {
        String normalized = message == null ? "" : message.trim().toLowerCase();
        return normalized.contains("这个") || normalized.contains("这款") || normalized.contains("它")
                || normalized.contains("干嘛") || normalized.contains("做什么") || normalized.contains("用途")
                || normalized.contains("适合");
    }

    private ChatResult saveAndReturn(String sessionId, String reply, List<Product> products,
                                     BudgetFeasibilityResult budgetResult, ProductSearchResult searchResult) {
        saveMessage(sessionId, "ASSISTANT", reply);
        return new ChatResult(reply, products, budgetResult, searchResult);
    }

    private void validate(ChatRequest request) {
        if (request == null || request.sessionId() == null || request.message() == null || request.message().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "sessionId 和 message 不能为空");
        }
        try {
            UUID.fromString(request.sessionId());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "sessionId 必须是 UUID");
        }
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
                + ", 类型: " + product.getProductType() + ", 品牌: " + product.getBrand()
                + ", 用途分类: " + product.getCategory() + ", 价格: " + product.getPrice()
                + "元, 标签与规格: " + product.getTags() + ", CPU: " + product.getCpu()
                + ", GPU: " + product.getGpu() + ", 内存: " + product.getMemoryGb()
                + "GB, 存储: " + product.getStorageGb() + "GB]";
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role);
        node.put("content", content);
        return node;
    }

    private void saveMessage(String sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    private String money(BigDecimal amount) {
        return amount == null ? "未知" : amount.stripTrailingZeros().toPlainString();
    }
}

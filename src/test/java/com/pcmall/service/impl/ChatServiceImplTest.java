package com.pcmall.service.impl;

import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.client.DeepSeekTimeoutException;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.ChatRequest;
import com.pcmall.entity.ChatMessage;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ChatMessageMapper;
import com.pcmall.mapper.ChatSessionMapper;
import com.pcmall.service.ProductService;
import com.pcmall.service.ProductSearchTool;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChatServiceImplTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Test
    void recordsUserAndAssistantMessagesForTextCompletion() {
        ChatSessionMapper sessionMapper = mock(ChatSessionMapper.class);
        ChatMessageMapper messageMapper = mock(ChatMessageMapper.class);
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.complete(any())).thenReturn(DeepSeekCompletion.text("推荐一台办公本"));
        ChatServiceImpl service = new ChatServiceImpl(sessionMapper, messageMapper, client, mock(ProductSearchTool.class), mock(ProductService.class), objectMapper);

        var result = service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "预算五千，想办公"));

        assertEquals("推荐一台办公本", result.reply());
        assertTrue(result.products().isEmpty());
        verify(messageMapper, times(2)).insert(any(ChatMessage.class));
    }

    @Test
    void returnsDatabaseProductsWhenModelCallsSearchProducts() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.complete(any())).thenReturn(DeepSeekCompletion.toolCall("call-1", "search_products", "{\"maxBudget\":5500,\"category\":\"办公/编程\",\"tags\":[\"轻薄\"]}"))
                .thenReturn(DeepSeekCompletion.text("这台适合你的预算"));
        Product product = new Product(); product.setId(1L); product.setName("灵越编程本 14"); product.setPrice(new BigDecimal("4599.00"));
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(List.of(product));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), mock(ChatMessageMapper.class), client, searchTool, mock(ProductService.class), objectMapper);

        var result = service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "预算5500，轻薄办公"));

        assertEquals("这台适合你的预算", result.reply());
        assertEquals(List.of(product), result.products());
    }

    @Test
    void supportsASecondProductToolCallBeforeTheFinalRecommendation() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.complete(any()))
                .thenReturn(DeepSeekCompletion.toolCall("call-1", "search_products", "{\"maxBudget\":6000,\"category\":\"办公/编程\",\"tags\":[]}"))
                .thenReturn(DeepSeekCompletion.toolCall("call-2", "search_products", "{\"maxBudget\":6000,\"category\":\"办公/编程\",\"tags\":[\"轻薄\"]}"))
                .thenReturn(DeepSeekCompletion.text("这两款都符合你的办公和轻薄需求"));
        Product first = new Product(); first.setId(1L); first.setName("灵越编程本 14");
        Product second = new Product(); second.setId(4L); second.setName("轻薄办公本 Air");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(List.of(first), List.of(second));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), mock(ChatMessageMapper.class), client, searchTool, mock(ProductService.class), objectMapper);

        var result = service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "预算6000，想要轻薄办公本"));

        assertEquals("这两款都符合你的办公和轻薄需求", result.reply());
        assertEquals(List.of(first, second), result.products());
        verify(client, times(3)).complete(any());
    }

    @Test
    void returnsFallbackProductsWhenDeepSeekTimesOut() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.complete(any())).thenThrow(new DeepSeekTimeoutException());
        Product product = new Product(); product.setId(2L); product.setName("游戏先锋 G15");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(List.of(product));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), mock(ChatMessageMapper.class), client, searchTool, mock(ProductService.class), objectMapper);

        var result = service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "推荐一台电脑"));

        assertEquals("当前推荐服务响应较慢，先为你展示热门商品。", result.reply());
        assertEquals(List.of(product), result.products());
    }

    @Test
    void suppliesOnlyServerReadProductFactsInTheContextPrompt() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        Product product = new Product();
        product.setId(2L); product.setName("游戏先锋 G15"); product.setPrice(new BigDecimal("5999.00"));
        product.setCpu("Intel Core i5-13450HX"); product.setGpu("NVIDIA GeForce RTX 4060");
        product.setMemoryGb(16); product.setStorageGb(1024); product.setStatus(1);
        ProductService productService = mock(ProductService.class);
        when(productService.getActiveById(2L)).thenReturn(product);
        when(client.complete(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<com.fasterxml.jackson.databind.node.ObjectNode> messages = invocation.getArgument(0, List.class);
            String systemPrompt = messages.get(0).path("content").asText();
            assertTrue(systemPrompt.contains("游戏先锋 G15"));
            assertTrue(systemPrompt.contains("5999.00"));
            assertTrue(systemPrompt.contains("RTX 4060"));
            return DeepSeekCompletion.text("可以流畅运行主流游戏。");
        });
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), mock(ChatMessageMapper.class), client,
                mock(ProductSearchTool.class), productService, objectMapper);

        service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "这台电脑怎么样", 2L));
    }

    @Test
    void rejectsAnUnknownContextProductBeforeCallingTheModel() {
        ProductService productService = mock(ProductService.class);
        when(productService.getActiveById(404L)).thenThrow(new BusinessException(HttpStatus.NOT_FOUND, "商品不存在或已下架"));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), mock(ChatMessageMapper.class),
                mock(DeepSeekClient.class), mock(ProductSearchTool.class), productService, objectMapper);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.chat(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "咨询这台", 404L)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }
}

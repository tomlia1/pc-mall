package com.pcmall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.client.DeepSeekTimeoutException;
import com.pcmall.dto.ChatRequest;
import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import com.pcmall.entity.ChatMessage;
import com.pcmall.mapper.ChatMessageMapper;
import com.pcmall.mapper.ChatSessionMapper;
import com.pcmall.service.BudgetFeasibilityService;
import com.pcmall.service.ProductSearchTool;
import com.pcmall.service.ProductService;
import com.pcmall.vo.BudgetFeasibilityResult;
import com.pcmall.vo.ProductSearchResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatServiceImplTest {
    private static final String SESSION_ID = "550e8400-e29b-41d4-a716-446655440000";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void keepsLaptopRouteAndOnlyReturnsLaptopCatalogProductsForOfficeRequest() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("这几款可用于日常办公。"));
        Product laptop = product(1L, "LAPTOP", "办公/编程", "4999");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.exact("符合条件", List.of(laptop)));
        BudgetFeasibilityService budgetService = mock(BudgetFeasibilityService.class);
        ChatServiceImpl service = service(client, searchTool, budgetService);

        var result = service.chat(new ChatRequest(SESSION_ID, "8000 办公"));

        var criteria = org.mockito.ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(searchTool).search(criteria.capture());
        assertEquals("LAPTOP", criteria.getValue().productType());
        assertEquals("办公/编程", criteria.getValue().category());
        assertEquals(new BigDecimal("8000"), criteria.getValue().maxBudget());
        assertEquals(List.of(laptop), result.products());
        verify(budgetService, never()).findForBudget(any());
    }

    @Test
    void doesNotTreatGamingLaptopAsCompleteBuildPlan() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("以下是游戏本。"));
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.exact("符合条件", List.of(product(2L, "LAPTOP", "游戏", "7999"))));
        BudgetFeasibilityService budgetService = mock(BudgetFeasibilityService.class);
        ChatServiceImpl service = service(client, searchTool, budgetService);

        service.chat(new ChatRequest(SESSION_ID, "8000 游戏本"));

        verify(budgetService, never()).findForBudget(any());
        var criteria = org.mockito.ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(searchTool).search(criteria.capture());
        assertEquals("LAPTOP", criteria.getValue().productType());
    }

    @Test
    void routesPubgBudgetToTheExistingBackendBudgetCalculation() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("当前库没有预算内完整独显方案。"));
        BudgetFeasibilityService budgetService = mock(BudgetFeasibilityService.class);
        BudgetFeasibilityResult budget = new BudgetFeasibilityResult(new BigDecimal("3000"), false,
                new BigDecimal("6223"), new BigDecimal("3223"), null);
        when(budgetService.findForBudget(new BigDecimal("3000"))).thenReturn(Optional.of(budget));
        ChatServiceImpl service = service(client, mock(ProductSearchTool.class), budgetService);

        var result = service.chat(new ChatRequest(SESSION_ID, "3000 吃鸡"));

        assertEquals(budget, result.budgetResult());
        verify(budgetService).findForBudget(new BigDecimal("3000"));
    }

    @Test
    void returnsFixedClarificationForUnspecifiedAccessoriesWithoutSearching() {
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        ChatServiceImpl service = service(mock(DeepSeekClient.class), searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "推荐配件"));

        assertTrue(result.reply().contains("CPU、显卡、主板"));
        assertTrue(result.products().isEmpty());
        verify(searchTool, never()).search(any());
    }

    @Test
    void returnsNoneWithoutCallingAiWhenTheCatalogHasNoMatchingProduct() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.none("当前商品库没有符合已确定条件的在售商品。"));
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "999999 显卡"));

        assertTrue(result.reply().contains("没有符合"));
        assertEquals("NONE", result.productSearchResult().matchType().name());
        verify(client, never()).completePlain(any());
    }

    @Test
    void preservesTheStructuredCatalogResultWhenAiTimesOutInsteadOfShowingPopularProducts() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenThrow(new DeepSeekTimeoutException());
        Product gpu = product(3L, "GPU", "DIY 装机", "4999");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.exact("符合条件", List.of(gpu)));
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "5000 显卡"));

        assertTrue(result.reply().contains("暂时不可用"));
        assertEquals(List.of(gpu), result.products());
        assertFalse(result.reply().contains("热门"));
    }

    @Test
    void removesModelClaimsThatContradictAnExactServerSideCatalogMatch() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("没有找到完全匹配的商品，但可以看看这些。"));
        Product laptop = product(4L, "LAPTOP", "办公/编程", "4999");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.exact("符合条件", List.of(laptop)));
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "8000 办公"));

        assertFalse(result.reply().contains("没有找到"));
        assertFalse(result.reply().contains("没有完全匹配"));
        assertEquals(List.of(laptop), result.products());
    }

    @Test
    void usesTheServerSideRelatedAlternativeExplanationWithoutCallingAi() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        Product laptop = product(5L, "LAPTOP", "办公/编程", "4999");
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        when(searchTool.search(any())).thenReturn(ProductSearchResult.alternative("保留硬约束后的相关备选", List.of(laptop)));
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "8000 CAD 笔记本"));

        assertTrue(result.reply().contains("相关备选"));
        assertTrue(result.reply().contains("相关备选"));
        assertEquals(List.of(laptop), result.products());
        verify(client, never()).completePlain(any());
    }

    @Test
    void ignoresUnknownModelToolCallsRatherThanTreatingThemAsProductSearch() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.toolCall("call-1", "unknown_tool", "{}"));
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "你好"));

        assertTrue(result.reply().contains("预算"));
        verify(searchTool, never()).search(any());
    }

    @Test
    void asksForUseAndDeviceTypeForBudgetOnlyQuestionWithoutCallingAiOrCatalogSearch() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        ChatServiceImpl service = service(client, searchTool, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "5000呢"));

        assertTrue(result.reply().contains("用途"));
        verify(client, never()).completePlain(any());
        verify(searchTool, never()).search(any());
    }

    @Test
    void inheritsTheLatestBudgetForLaptopFollowUpInsteadOfSearchingMiniPcsOrAllProducts() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("以下是笔记本。"));
        ProductSearchTool searchTool = mock(ProductSearchTool.class);
        Product laptop = product(6L, "LAPTOP", "办公/编程", "4999");
        when(searchTool.search(any())).thenReturn(ProductSearchResult.exact("符合条件", List.of(laptop)));
        ChatMessage prior = new ChatMessage();
        prior.setRole("USER");
        prior.setContent("5000呢");
        prior.setCreatedAt(LocalDateTime.now());
        ChatMessageMapper messages = mock(ChatMessageMapper.class);
        when(messages.selectList(any())).thenReturn(List.of(prior));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), messages, client, searchTool,
                mock(ProductService.class), objectMapper, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "笔记本也没有？"));

        var criteria = org.mockito.ArgumentCaptor.forClass(ProductSearchCriteria.class);
        verify(searchTool).search(criteria.capture());
        assertEquals("LAPTOP", criteria.getValue().productType());
        assertEquals(new BigDecimal("5000"), criteria.getValue().maxBudget());
        assertEquals(List.of(laptop), result.products());
    }

    @Test
    void explainsTheCurrentDetailProductInsteadOfRepeatingBudgetClarification() {
        DeepSeekClient client = mock(DeepSeekClient.class);
        when(client.completePlain(any())).thenReturn(DeepSeekCompletion.text("这是用于图形输出和游戏画面的显卡。"));
        Product currentProduct = product(15L, "GPU", "DIY 装机", "1399");
        currentProduct.setName("ASUS Dual GeForce RTX 3050 6GB 显卡");
        ProductService products = mock(ProductService.class);
        when(products.getActiveById(15L)).thenReturn(currentProduct);
        ChatMessage prior = new ChatMessage();
        prior.setRole("USER");
        prior.setContent("2000可以干嘛");
        prior.setCreatedAt(LocalDateTime.now());
        ChatMessageMapper messages = mock(ChatMessageMapper.class);
        when(messages.selectList(any())).thenReturn(List.of(prior));
        ChatServiceImpl service = new ChatServiceImpl(mock(ChatSessionMapper.class), messages, client,
                mock(ProductSearchTool.class), products, objectMapper, mock(BudgetFeasibilityService.class));

        var result = service.chat(new ChatRequest(SESSION_ID, "这个是干嘛的", 15L));

        assertEquals("这是用于图形输出和游戏画面的显卡。", result.reply());
        verify(client).completePlain(any());
    }

    private ChatServiceImpl service(DeepSeekClient client, ProductSearchTool searchTool, BudgetFeasibilityService budgetService) {
        ChatMessageMapper messages = mock(ChatMessageMapper.class);
        when(messages.selectList(any())).thenReturn(List.of());
        return new ChatServiceImpl(mock(ChatSessionMapper.class), messages, client, searchTool,
                mock(ProductService.class), objectMapper, budgetService);
    }

    private Product product(long id, String type, String category, String price) {
        Product product = new Product();
        product.setId(id);
        product.setProductType(type);
        product.setCategory(category);
        product.setStatus(1);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}

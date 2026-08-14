package com.pcmall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BuildCheckIssue;
import com.pcmall.vo.BuildCheckResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BuildAnalysisServiceImplTest {

    @Test
    void returnsIncompleteCheckWithoutCallingDeepSeekOrGeneratingCandidates() {
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        ProductMapper products = mock(ProductMapper.class);
        DeepSeekClient deepSeek = mock(DeepSeekClient.class);
        BuildCheckResult incomplete = new BuildCheckResult("INCOMPLETE", List.of("GPU", "RAM"), List.of(), BigDecimal.ZERO, null, null);
        when(compatibility.check(any())).thenReturn(incomplete);

        var result = new BuildAnalysisServiceImpl(compatibility, products, deepSeek, new ObjectMapper()).analyze(incompleteRequest());

        assertEquals(incomplete, result.check());
        assertNull(result.reply());
        assertTrue(result.candidates().isEmpty());
        assertFalse(result.aiUnavailable());
        verifyNoInteractions(products, deepSeek);
    }

    @Test
    void givesDeepSeekOnlyBackendFactsAndEmitsCompatibleDatabaseCandidate() {
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        ProductMapper products = mock(ProductMapper.class);
        DeepSeekClient deepSeek = mock(DeepSeekClient.class);
        BuildCheckResult error = new BuildCheckResult("ERROR", List.of(), List.of(new BuildCheckIssue("ERROR", "GPU_CASE_LENGTH", "显卡长度超过机箱支持长度。")), BigDecimal.valueOf(6999), 383, 460);
        BuildCheckResult pass = new BuildCheckResult("PASS", List.of(), List.of(), BigDecimal.valueOf(7899), 383, 460);
        when(compatibility.check(any())).thenAnswer(invocation -> ((BuildCheckRequest) invocation.getArgument(0)).gpuId().equals(8L) ? pass : error);
        when(products.selectBatchIds(anyCollection())).thenReturn(selectedProducts());
        when(products.selectList(any())).thenReturn(List.of(product(8L, "GPU", "数据库候选显卡")));
        when(deepSeek.completePlain(any())).thenReturn(DeepSeekCompletion.text("先处理显卡长度问题。"));

        var result = new BuildAnalysisServiceImpl(compatibility, products, deepSeek, new ObjectMapper()).analyze(completeRequest());

        assertEquals(error, result.check());
        assertEquals("先处理显卡长度问题。", result.reply());
        assertEquals(List.of(8L), result.candidates().stream().map(Product::getId).toList());
        ArgumentCaptor<List<ObjectNode>> messages = ArgumentCaptor.forClass(List.class);
        verify(deepSeek).completePlain(messages.capture());
        assertTrue(messages.getValue().get(0).path("content").asText().contains("数据库候选显卡"));
        assertTrue(messages.getValue().get(0).path("content").asText().contains("GPU_CASE_LENGTH"));
        assertTrue(messages.getValue().get(0).path("content").asText().contains("不得自行判断硬件能否运行"));
    }

    @Test
    void keepsCheckAndCandidatesWhenDeepSeekIsUnavailable() {
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        ProductMapper products = mock(ProductMapper.class);
        DeepSeekClient deepSeek = mock(DeepSeekClient.class);
        BuildCheckResult warning = new BuildCheckResult("WARNING", List.of(), List.of(new BuildCheckIssue("WARNING", "PSU_POWER_HEADROOM", "电源只是估算建议。")), BigDecimal.valueOf(6999), 383, 460);
        BuildCheckResult pass = new BuildCheckResult("PASS", List.of(), List.of(), BigDecimal.valueOf(7199), 383, 460);
        when(compatibility.check(any())).thenAnswer(invocation -> ((BuildCheckRequest) invocation.getArgument(0)).psuId().equals(8L) ? pass : warning);
        when(products.selectBatchIds(anyCollection())).thenReturn(selectedProducts());
        when(products.selectList(any())).thenReturn(List.of(product(8L, "PSU", "数据库候选电源")));
        when(deepSeek.completePlain(any())).thenThrow(new RuntimeException("upstream unavailable"));

        var result = new BuildAnalysisServiceImpl(compatibility, products, deepSeek, new ObjectMapper()).analyze(completeRequest());

        assertEquals(warning, result.check());
        assertTrue(result.aiUnavailable());
        assertNull(result.reply());
        assertEquals(List.of(8L), result.candidates().stream().map(Product::getId).toList());
    }

    @Test
    void removesModelCompatibilityClaimsThatAreNotBackedByTheStructuredCheck() {
        String reply = BuildAnalysisServiceImpl.sanitizeReply("## 结论\n当前配置可以装机。\n后端仅提示电源余量是估算建议。\n可以继续搭配。\n短期内可继续使用。\n其他部件无需调整。\n不需要更换配件。");

        assertEquals("结论\n后端仅提示电源余量是估算建议。", reply);
    }

    @Test
    void removesPassClaimWhenBackendCheckIsWarning() {
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        ProductMapper products = mock(ProductMapper.class);
        DeepSeekClient deepSeek = mock(DeepSeekClient.class);
        BuildCheckResult warning = new BuildCheckResult("WARNING", List.of(), List.of(new BuildCheckIssue("WARNING", "PSU_POWER_HEADROOM", "电源余量为估算建议。")), BigDecimal.valueOf(6999), 383, 460);
        when(compatibility.check(any())).thenReturn(warning);
        when(products.selectBatchIds(anyCollection())).thenReturn(selectedProducts());
        when(products.selectList(any())).thenReturn(List.of());
        when(deepSeek.completePlain(any())).thenReturn(DeepSeekCompletion.text("整机配置为 PASS 级，完全兼容。\n后端检查提示电源余量是估算建议。"));

        var result = new BuildAnalysisServiceImpl(compatibility, products, deepSeek, new ObjectMapper()).analyze(completeRequest());

        assertEquals(warning, result.check());
        assertEquals("后端检查提示电源余量是估算建议。", result.reply());
        assertFalse(result.reply().contains("PASS"));
    }

    private BuildCheckRequest incompleteRequest() { return new BuildCheckRequest(1L, 2L, null, null, 5L, 6L, 7L); }
    private BuildCheckRequest completeRequest() { return new BuildCheckRequest(1L, 2L, 3L, 4L, 5L, 6L, 7L); }
    private List<Product> selectedProducts() { return List.of(product(1L, "CPU", "处理器"), product(2L, "MOTHERBOARD", "主板"), product(3L, "GPU", "当前显卡"), product(4L, "RAM", "内存"), product(5L, "SSD", "固态硬盘"), product(6L, "PSU", "电源"), product(7L, "CASE", "机箱")); }
    private Product product(long id, String type, String name) {
        Product product = new Product();
        product.setId(id); product.setProductType(type); product.setName(name); product.setBrand("测试品牌");
        product.setStatus(1); product.setPrice(BigDecimal.valueOf(999)); product.setTags("测试规格");
        return product;
    }
}

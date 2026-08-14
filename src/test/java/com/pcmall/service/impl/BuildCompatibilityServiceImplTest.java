package com.pcmall.service.impl;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.common.BusinessException;
import com.pcmall.entity.Product;
import com.pcmall.entity.ProductHardwareSpec;
import com.pcmall.mapper.ProductHardwareSpecMapper;
import com.pcmall.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuildCompatibilityServiceImplTest {

    @Test
    void reportsMissingSlotsWithoutClaimingCompatibilityPass() {
        BuildCompatibilityServiceImpl service = new BuildCompatibilityServiceImpl(mock(ProductMapper.class), mock(ProductHardwareSpecMapper.class));

        var result = service.check(new BuildCheckRequest(null, null, null, null, null, null, null));

        assertEquals("INCOMPLETE", result.status());
        assertTrue(result.missingSlots().contains("MOTHERBOARD"));
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void reportsSocketMismatchAsAnError() {
        ProductMapper products = mock(ProductMapper.class);
        ProductHardwareSpecMapper specs = mock(ProductHardwareSpecMapper.class);
        when(products.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(1L, "CPU"), product(2L, "MOTHERBOARD"), product(3L, "GPU"), product(4L, "RAM"),
                product(5L, "SSD"), product(6L, "PSU"), product(7L, "CASE")));
        when(specs.selectBatchIds(anyCollection())).thenReturn(List.of(
                spec(1L, "AM5", null, null, null, null, null, null, null, null, 88),
                spec(2L, "LGA1700", "DDR5", "mATX", null, null, null, null, "M.2 NVMe", null, null),
                spec(3L, null, null, null, null, 227, null, null, null, null, 115),
                spec(4L, null, "DDR5", null, null, null, null, null, null, null, null),
                spec(5L, null, null, null, null, null, null, "M.2 NVMe", null, null, null),
                spec(6L, null, null, null, null, null, null, null, null, 650, null),
                spec(7L, null, null, null, "mATX", null, 330, null, null, null, null)));

        var result = new BuildCompatibilityServiceImpl(products, specs).check(completeRequest());

        assertEquals("ERROR", result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.rule().equals("CPU_MOTHERBOARD_SOCKET") && issue.level().equals("ERROR")));
    }

    @Test
    void rejectsAProductWhoseTypeDoesNotMatchItsSubmittedSlot() {
        ProductMapper products = mock(ProductMapper.class);
        ProductHardwareSpecMapper specs = mock(ProductHardwareSpecMapper.class);
        Product wrongType = product(1L, "LAPTOP");
        when(products.selectBatchIds(anyCollection())).thenReturn(List.of(
                wrongType, product(2L, "MOTHERBOARD"), product(3L, "GPU"), product(4L, "RAM"),
                product(5L, "SSD"), product(6L, "PSU"), product(7L, "CASE")));

        assertThrows(BusinessException.class, () -> new BuildCompatibilityServiceImpl(products, specs).check(completeRequest()));
    }

    @Test
    void warnsOnlyWhenPsuIsBelowTheRecommendedPower() {
        ProductMapper products = mock(ProductMapper.class);
        ProductHardwareSpecMapper specs = mock(ProductHardwareSpecMapper.class);
        when(products.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(1L, "CPU"), product(2L, "MOTHERBOARD"), product(3L, "GPU"), product(4L, "RAM"),
                product(5L, "SSD"), product(6L, "PSU"), product(7L, "CASE")));
        when(specs.selectBatchIds(anyCollection())).thenReturn(List.of(
                spec(1L, "LGA1700", null, null, null, null, null, null, null, null, 148),
                spec(2L, "LGA1700", "DDR4", "mATX", null, null, null, null, "M.2 NVMe", null, null),
                spec(3L, null, null, null, null, 261, null, null, null, null, 220),
                spec(4L, null, "DDR4", null, null, null, null, null, null, null, null),
                spec(5L, null, null, null, null, null, null, "M.2 NVMe", null, null, null),
                spec(6L, null, null, null, null, null, null, null, null, 400, null),
                spec(7L, null, null, null, "mATX,ATX", null, 365, null, null, null, null)));

        var result = new BuildCompatibilityServiceImpl(products, specs).check(completeRequest());

        assertEquals(586, result.recommendedPsuW());
        assertEquals("WARNING", result.status());
        assertTrue(result.issues().stream().anyMatch(issue -> issue.rule().equals("PSU_POWER_HEADROOM") && issue.level().equals("WARNING")));
    }

    @Test
    void reportsMemoryGpuCaseAndSsdMismatchesAsErrors() {
        ProductMapper products = mock(ProductMapper.class);
        ProductHardwareSpecMapper specs = mock(ProductHardwareSpecMapper.class);
        when(products.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(1L, "CPU"), product(2L, "MOTHERBOARD"), product(3L, "GPU"), product(4L, "RAM"),
                product(5L, "SSD"), product(6L, "PSU"), product(7L, "CASE")));
        when(specs.selectBatchIds(anyCollection())).thenReturn(List.of(
                spec(1L, "LGA1700", null, null, null, null, null, null, null, null, 148),
                spec(2L, "LGA1700", "DDR4", "ATX", null, null, null, null, "M.2 NVMe", null, null),
                spec(3L, null, null, null, null, 348, null, null, null, null, 220),
                spec(4L, null, "DDR5", null, null, null, null, null, null, null, null),
                spec(5L, null, null, null, null, null, null, "SATA 2.5", null, null, null),
                spec(6L, null, null, null, null, null, null, null, null, 750, null),
                spec(7L, null, null, null, "mATX", null, 330, null, null, null, null)));

        var result = new BuildCompatibilityServiceImpl(products, specs).check(completeRequest());
        var rules = result.issues().stream().map(issue -> issue.rule()).toList();

        assertEquals("ERROR", result.status());
        assertTrue(rules.contains("MOTHERBOARD_RAM_MEMORY_TYPE"));
        assertTrue(rules.contains("GPU_CASE_LENGTH"));
        assertTrue(rules.contains("MOTHERBOARD_CASE_FORM_FACTOR"));
        assertTrue(rules.contains("SSD_MOTHERBOARD_INTERFACE"));
    }

    @Test
    void passesWhenA400WPowerSupplyExceedsThe388WRecommendation() {
        ProductMapper products = mock(ProductMapper.class);
        ProductHardwareSpecMapper specs = mock(ProductHardwareSpecMapper.class);
        when(products.selectBatchIds(anyCollection())).thenReturn(List.of(
                product(1L, "CPU"), product(2L, "MOTHERBOARD"), product(3L, "GPU"), product(4L, "RAM"),
                product(5L, "SSD"), product(6L, "PSU"), product(7L, "CASE")));
        when(specs.selectBatchIds(anyCollection())).thenReturn(List.of(
                spec(1L, "LGA1700", null, null, null, null, null, null, null, null, 88),
                spec(2L, "LGA1700", "DDR4", "mATX", null, null, null, null, "M.2 NVMe", null, null),
                spec(3L, null, null, null, null, 261, null, null, null, null, 115),
                spec(4L, null, "DDR4", null, null, null, null, null, null, null, null),
                spec(5L, null, null, null, null, null, null, "M.2 NVMe", null, null, null),
                spec(6L, null, null, null, null, null, null, null, null, 400, null),
                spec(7L, null, null, null, "mATX,ATX", null, 365, null, null, null, null)));

        var result = new BuildCompatibilityServiceImpl(products, specs).check(completeRequest());

        assertEquals(388, result.recommendedPsuW());
        assertEquals("PASS", result.status());
        assertTrue(result.issues().isEmpty());
    }

    private BuildCheckRequest completeRequest() { return new BuildCheckRequest(1L, 2L, 3L, 4L, 5L, 6L, 7L); }

    private Product product(long id, String type) {
        Product product = new Product();
        product.setId(id);
        product.setProductType(type);
        product.setStatus(1);
        product.setPrice(BigDecimal.valueOf(100));
        return product;
    }

    private ProductHardwareSpec spec(long productId, String socket, String memoryType, String formFactor, String supportedFormFactors,
                                     Integer gpuLength, Integer caseGpuMaxLength, String storageInterface, String supportedStorageInterfaces,
                                     Integer psuWatt, Integer estimatedPowerW) {
        ProductHardwareSpec spec = new ProductHardwareSpec();
        spec.setProductId(productId); spec.setCpuSocket(socket); spec.setMemoryType(memoryType); spec.setFormFactor(formFactor);
        spec.setSupportedFormFactors(supportedFormFactors); spec.setGpuLengthMm(gpuLength); spec.setCaseGpuMaxLengthMm(caseGpuMaxLength);
        spec.setStorageInterface(storageInterface); spec.setSupportedStorageInterfaces(supportedStorageInterfaces);
        spec.setPsuWatt(psuWatt); spec.setEstimatedPowerW(estimatedPowerW);
        return spec;
    }
}

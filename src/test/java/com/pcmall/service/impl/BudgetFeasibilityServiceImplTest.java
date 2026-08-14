package com.pcmall.service.impl;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BuildCheckResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BudgetFeasibilityServiceImplTest {
    @Test
    void calculatesBudgetPlanOnlyWhenTheCallerHasAlreadyChosenBuildPlanRoute() {
        BudgetFeasibilityServiceImpl service = service(products());

        var result = service.findForBudget(new BigDecimal("3000")).orElseThrow();

        assertEquals(new BigDecimal("3000"), result.budget());
        assertFalse(result.hasWithinBudgetPlan());
        assertEquals(new BigDecimal("6223"), result.minimumFeasibleTotal());
        assertEquals(new BigDecimal("3223"), result.budgetGap());
        assertEquals(null, result.candidate());
    }

    @Test
    void returnsTheDeterministicLowestPassingBuildWhenBudgetIsEnough() {
        List<Product> products = products();
        products.add(product(10L, "CPU", "1299"));
        BudgetFeasibilityServiceImpl service = service(products);

        var result = service.findForBudget(new BigDecimal("7000")).orElseThrow();

        assertTrue(result.hasWithinBudgetPlan());
        assertEquals(new BigDecimal("6223"), result.minimumFeasibleTotal());
        assertEquals(BigDecimal.ZERO, result.budgetGap());
        assertEquals(7, result.candidate().products().size());
        assertEquals(1L, result.candidate().build().cpuId());
        assertEquals("PASS", result.candidate().check().status());
    }

    @Test
    void stopsAfterTheFirstLowestPricePassingCombinationInsteadOfScanningEveryCombination() {
        List<Product> products = products();
        products.add(product(8L, "CPU", "1399"));
        products.add(product(9L, "GPU", "2499"));
        ProductMapper productMapper = mock(ProductMapper.class);
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        when(productMapper.selectList(any())).thenReturn(products);
        when(compatibility.check(any(BuildCheckRequest.class))).thenReturn(
                new BuildCheckResult("PASS", List.of(), List.of(), BigDecimal.ZERO, 320, 384));

        BudgetFeasibilityServiceImpl service = new BudgetFeasibilityServiceImpl(productMapper, compatibility);
        service.findForBudget(new BigDecimal("7000"));

        verify(compatibility, times(1)).check(any(BuildCheckRequest.class));
    }

    @Test
    void noLongerTreatsGamingOrGpuWordsAsAnIndependentBuildPlanSignal() {
        BudgetFeasibilityServiceImpl service = service(products());

        assertTrue(service.findForMessage("预算3000元，吃鸡").isEmpty());
        assertTrue(service.findForMessage("5000 显卡").isEmpty());
    }

    private BudgetFeasibilityServiceImpl service(List<Product> products) {
        ProductMapper productMapper = mock(ProductMapper.class);
        BuildCompatibilityService compatibility = mock(BuildCompatibilityService.class);
        when(productMapper.selectList(any())).thenReturn(products);
        when(compatibility.check(any(BuildCheckRequest.class))).thenReturn(
                new BuildCheckResult("PASS", List.of(), List.of(), BigDecimal.ZERO, 320, 384));
        return new BudgetFeasibilityServiceImpl(productMapper, compatibility);
    }

    private List<Product> products() {
        List<Product> products = new ArrayList<>();
        products.add(product(1L, "CPU", "1299"));
        products.add(product(2L, "MOTHERBOARD", "799"));
        products.add(product(3L, "GPU", "2399"));
        products.add(product(4L, "RAM", "499"));
        products.add(product(5L, "SSD", "499"));
        products.add(product(6L, "PSU", "329"));
        products.add(product(7L, "CASE", "399"));
        return products;
    }

    private Product product(long id, String type, String price) {
        Product product = new Product();
        product.setId(id);
        product.setProductType(type);
        product.setStatus(1);
        product.setPrice(new BigDecimal(price));
        return product;
    }
}

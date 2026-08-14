package com.pcmall.service.impl;

import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.vo.ProductMatchType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchToolContractTest {
    @Test
    void returnsNoneInsteadOfUnfilteredPopularProductsWhenTheStrictProductTypeQueryIsEmpty() {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of());
        ProductSearchToolImpl tool = new ProductSearchToolImpl(mapper);

        var result = tool.search(new ProductSearchCriteria(null, new BigDecimal("5000"), "GPU", null, "不存在的型号", List.of()));

        assertEquals(ProductMatchType.NONE, result.matchType());
        assertEquals(List.of(), result.products());
    }

    @Test
    void keepsTypeBudgetAndCategoryWhenRelaxingOnlyAKeywordPreference() {
        ProductMapper mapper = mock(ProductMapper.class);
        Product laptop = new Product();
        laptop.setId(7L);
        when(mapper.selectList(any())).thenReturn(List.of(), List.of(laptop));
        ProductSearchToolImpl tool = new ProductSearchToolImpl(mapper);

        var result = tool.search(new ProductSearchCriteria(null, new BigDecimal("8000"), "LAPTOP", "办公/编程", "偏门用途", List.of()));

        assertEquals(ProductMatchType.ALTERNATIVE, result.matchType());
        assertEquals(List.of(laptop), result.products());
        verify(mapper, times(2)).selectList(any());
    }

    @Test
    void refusesToQueryTheCatalogWhenTheProductTypeIsMissing() {
        ProductMapper mapper = mock(ProductMapper.class);
        ProductSearchToolImpl tool = new ProductSearchToolImpl(mapper);

        var result = tool.search(new ProductSearchCriteria(null, new BigDecimal("8000"), null, null, null, List.of()));

        assertEquals(ProductMatchType.NONE, result.matchType());
        verify(mapper, times(0)).selectList(any());
    }
}

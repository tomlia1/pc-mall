package com.pcmall.service.impl;

import com.pcmall.dto.ProductSearchCriteria;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductSearchToolImplTest {
    @Test
    void buildsActiveTypedQueryFromBudgetCategoryAndTags() {
        ProductMapper mapper = mock(ProductMapper.class);
        Product product = new Product();
        product.setId(1L);
        when(mapper.selectList(any())).thenReturn(List.of(product));
        ProductSearchToolImpl tool = new ProductSearchToolImpl(mapper);

        var result = tool.search(new ProductSearchCriteria(null, new BigDecimal("5500"), "LAPTOP", "办公/编程", null, List.of("轻薄", "办公")));

        assertEquals(List.of(product), result.products());
        assertEquals("EXACT", result.matchType().name());
        verify(mapper).selectList(any());
    }
}

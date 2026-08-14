package com.pcmall.service.impl;

import com.pcmall.common.BusinessException;
import com.pcmall.dto.ProductQuery;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    @Test
    void exposesProductTypeOnCatalogProductsAndQueries() {
        assertDoesNotThrow(() -> Product.class.getMethod("getProductType"));
        assertDoesNotThrow(() -> ProductQuery.class.getMethod("productType"));
    }

    @Test
    void returnsAnActiveProductForItsDetailPage() {
        ProductMapper mapper = mock(ProductMapper.class);
        Product product = product(102L, 1);
        when(mapper.selectById(102L)).thenReturn(product);

        Product actual = new ProductServiceImpl(mapper).getActiveById(102L);

        assertSame(product, actual);
    }

    @Test
    void rejectsAnInactiveProductForItsDetailPage() {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectById(102L)).thenReturn(product(102L, 0));

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductServiceImpl(mapper).getActiveById(102L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void rejectsAnUnknownProductForItsDetailPage() {
        ProductMapper mapper = mock(ProductMapper.class);
        when(mapper.selectById(404L)).thenReturn(null);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductServiceImpl(mapper).getActiveById(404L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatus());
    }

    @Test
    void rejectsAProductSearchWhoseMinimumPriceExceedsMaximumPrice() {
        ProductMapper mapper = mock(ProductMapper.class);
        ProductQuery query = new ProductQuery(new BigDecimal("8000"), new BigDecimal("5000"),
                null, null, null, null, null, 1, 12);

        BusinessException error = assertThrows(BusinessException.class,
                () -> new ProductServiceImpl(mapper).search(query));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatus());
    }

    @Test
    void returnsTheTotalAndTotalPagesAlongsideTheCurrentProductPage() throws Exception {
        ProductMapper mapper = mock(ProductMapper.class);
        Page<Product> databasePage = new Page<>(1, 9);
        databasePage.setTotal(102);
        databasePage.setRecords(List.of(product(1L, 1)));
        when(mapper.selectPage(any(Page.class), any())).thenReturn(databasePage);

        Object result = new ProductServiceImpl(mapper).search(new ProductQuery(null, null, null, null, null, null, null, 1, 9));

        assertEquals(102L, result.getClass().getMethod("total").invoke(result));
        assertEquals(12L, result.getClass().getMethod("totalPages").invoke(result));
    }

    private Product product(long id, int status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        return product;
    }
}

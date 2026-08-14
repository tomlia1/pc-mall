package com.pcmall.vo;

import com.pcmall.entity.Product;

import java.util.List;

public record ProductSearchResult(ProductMatchType matchType, String reason, List<Product> products) {
    public ProductSearchResult {
        products = products == null ? List.of() : List.copyOf(products);
    }

    public static ProductSearchResult exact(String reason, List<Product> products) {
        return new ProductSearchResult(ProductMatchType.EXACT, reason, products);
    }

    public static ProductSearchResult alternative(String reason, List<Product> products) {
        return new ProductSearchResult(ProductMatchType.ALTERNATIVE, reason, products);
    }

    public static ProductSearchResult none(String reason) {
        return new ProductSearchResult(ProductMatchType.NONE, reason, List.of());
    }
}

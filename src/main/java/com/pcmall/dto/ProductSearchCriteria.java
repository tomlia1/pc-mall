package com.pcmall.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchCriteria(
        BigDecimal minBudget,
        BigDecimal maxBudget,
        String productType,
        String category,
        String keyword,
        List<String> tags) {
    public ProductSearchCriteria {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}

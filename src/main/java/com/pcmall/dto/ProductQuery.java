package com.pcmall.dto;
import java.math.BigDecimal;
public record ProductQuery(BigDecimal minPrice, BigDecimal maxPrice, String category, String brand,
                           String keyword, String tag, String productType, int page, int pageSize) { }

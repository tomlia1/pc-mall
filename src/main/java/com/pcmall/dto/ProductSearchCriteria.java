package com.pcmall.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductSearchCriteria(BigDecimal maxBudget, String category, List<String> tags) { }

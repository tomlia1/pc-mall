package com.pcmall.vo;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;

import java.math.BigDecimal;
import java.util.List;

public record BudgetBuildCandidate(BuildCheckRequest build, List<Product> products, BigDecimal totalPrice,
                                   BuildCheckResult check) { }

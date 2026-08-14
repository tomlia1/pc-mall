package com.pcmall.vo;

import java.math.BigDecimal;

public record BudgetFeasibilityResult(BigDecimal budget, boolean hasWithinBudgetPlan,
                                      BigDecimal minimumFeasibleTotal, BigDecimal budgetGap,
                                      BudgetBuildCandidate candidate) { }

package com.pcmall.service;

import com.pcmall.vo.BudgetFeasibilityResult;

import java.math.BigDecimal;
import java.util.Optional;

public interface BudgetFeasibilityService {
    Optional<BudgetFeasibilityResult> findForMessage(String message);
    Optional<BudgetFeasibilityResult> findForBudget(BigDecimal budget);
}

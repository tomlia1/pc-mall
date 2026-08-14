package com.pcmall.vo;

import com.pcmall.entity.Product;
import java.util.List;

public record ChatResult(String reply, List<Product> products, BudgetFeasibilityResult budgetResult,
                         ProductSearchResult productSearchResult) {
    public ChatResult(String reply, List<Product> products) {
        this(reply, products, null, null);
    }

    public ChatResult(String reply, List<Product> products, BudgetFeasibilityResult budgetResult) {
        this(reply, products, budgetResult, null);
    }
}

package com.pcmall.service.impl;

import com.pcmall.dto.ProductSearchCriteria;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Lightweight, deterministic routing performed before any model request. */
final class ChatIntentRouter {
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{3,6})(?!\\d)");
    private static final Pattern RANGE = Pattern.compile("(?<!\\d)(\\d{3,6})\\s*(?:-|~|到|至)\\s*(\\d{3,6})(?!\\d)");
    private static final Map<String, String> PART_TYPES = Map.ofEntries(
            Map.entry("显卡", "GPU"), Map.entry("gpu", "GPU"), Map.entry("cpu", "CPU"),
            Map.entry("主板", "MOTHERBOARD"), Map.entry("内存", "RAM"), Map.entry("ssd", "SSD"),
            Map.entry("硬盘", "SSD"), Map.entry("电源", "PSU"), Map.entry("机箱", "CASE"));

    enum Route { LAPTOP_SEARCH, PART_SEARCH, BUILD_PLAN, CLARIFY, BUDGET_CLARIFY, GENERAL }

    record RoutedIntent(Route route, ProductSearchCriteria criteria) { }

    RoutedIntent route(String message) {
        return route(message, List.of());
    }

    RoutedIntent route(String message, List<String> previousUserMessages) {
        String normalized = message == null ? "" : message.toLowerCase(Locale.ROOT).trim();
        BudgetRange budget = budget(normalized);
        if (!budget.hasValue()) budget = latestBudget(previousUserMessages);

        if (containsAny(normalized, "笔记本", "游戏本", "办公本", "轻薄本")) {
            return new RoutedIntent(Route.LAPTOP_SEARCH, laptopCriteria(normalized, budget));
        }
        String partType = partType(normalized);
        if (partType != null) {
            List<String> tags = containsAny(normalized, "游戏", "电竞", "吃鸡", "3a") ? List.of("游戏") : List.of();
            return new RoutedIntent(Route.PART_SEARCH,
                    new ProductSearchCriteria(budget.min(), budget.max(), partType, null, null, tags));
        }
        if (containsAny(normalized, "diy 装机", "diy装机", "装机", "配主机", "配台式机", "组一台主机")) {
            return new RoutedIntent(Route.BUILD_PLAN, budget.criteria());
        }
        if (budget.max() != null && containsAny(normalized, "游戏", "电竞", "吃鸡", "3a")) {
            return new RoutedIntent(Route.BUILD_PLAN, budget.criteria());
        }
        if (containsAny(normalized, "办公", "学习", "编程", "写代码", "文档", "表格", "设计", "剪辑", "建模", "渲染", "创作")) {
            return new RoutedIntent(Route.LAPTOP_SEARCH, laptopCriteria(normalized, budget));
        }
        if (normalized.contains("推荐配件")) {
            return new RoutedIntent(Route.CLARIFY, null);
        }
        if (budget.hasValue()) return new RoutedIntent(Route.BUDGET_CLARIFY, budget.criteria());
        return new RoutedIntent(Route.GENERAL, null);
    }

    private ProductSearchCriteria laptopCriteria(String message, BudgetRange budget) {
        String category = laptopCategory(message);
        String keyword = category == null ? nicheKeyword(message) : null;
        if ("也没有".equals(keyword)) keyword = null;
        return new ProductSearchCriteria(budget.min(), budget.max(), "LAPTOP", category, keyword, List.of());
    }

    private String laptopCategory(String message) {
        if (containsAny(message, "办公", "学习", "编程", "写代码", "文档", "表格")) return "办公/编程";
        if (containsAny(message, "设计", "剪辑", "建模", "渲染", "创作")) return "设计/创作";
        if (containsAny(message, "游戏", "电竞", "吃鸡", "3a")) return "游戏";
        return null;
    }

    private String partType(String message) {
        return PART_TYPES.entrySet().stream().filter(entry -> message.contains(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private String nicheKeyword(String message) {
        String keyword = AMOUNT.matcher(message).replaceAll("");
        for (String word : List.of("笔记本", "游戏本", "办公本", "轻薄本", "推荐", "想要", "需要", "预算", "人民币", "元", "块")) {
            keyword = keyword.replace(word, "");
        }
        keyword = keyword.replaceAll("[\\s,，。！？!?]", "");
        return keyword.isBlank() ? null : keyword;
    }

    private boolean containsAny(String message, String... words) {
        for (String word : words) if (message.contains(word)) return true;
        return false;
    }

    private BudgetRange budget(String message) {
        Matcher range = RANGE.matcher(message);
        if (range.find()) return new BudgetRange(new BigDecimal(range.group(1)), new BigDecimal(range.group(2)));
        Matcher amount = AMOUNT.matcher(message);
        if (amount.find()) return new BudgetRange(null, new BigDecimal(amount.group(1)));
        return new BudgetRange(null, null);
    }

    private record BudgetRange(BigDecimal min, BigDecimal max) {
        boolean hasValue() { return min != null || max != null; }
        ProductSearchCriteria criteria() { return new ProductSearchCriteria(min, max, null, null, null, List.of()); }
    }

    private BudgetRange latestBudget(List<String> previousUserMessages) {
        if (previousUserMessages == null) return new BudgetRange(null, null);
        for (int index = previousUserMessages.size() - 1; index >= 0; index--) {
            BudgetRange prior = budget(previousUserMessages.get(index).toLowerCase(Locale.ROOT));
            if (prior.hasValue()) return prior;
        }
        return new BudgetRange(null, null);
    }
}

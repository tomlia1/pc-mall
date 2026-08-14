package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.BudgetFeasibilityService;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BudgetBuildCandidate;
import com.pcmall.vo.BudgetFeasibilityResult;
import com.pcmall.vo.BuildCheckResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BudgetFeasibilityServiceImpl implements BudgetFeasibilityService {
    private static final Pattern EXPLICIT_BUILD_INTENT = Pattern.compile("DIY|装机|配主机|配台式机|组一台主机", Pattern.CASE_INSENSITIVE);
    private static final List<String> SLOT_TYPES = List.of("CPU", "MOTHERBOARD", "GPU", "RAM", "SSD", "PSU", "CASE");
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d{3,6})(?!\\d)");
    private static final Pattern BUILD_INTENT = Pattern.compile("DIY|装机|主机|游戏|吃鸡|显卡|独显", Pattern.CASE_INSENSITIVE);
    private static final Pattern BUDGET_CUE = Pattern.compile("预算|人民币|¥|￥|元|块|rmb", Pattern.CASE_INSENSITIVE);

    private final ProductMapper productMapper;
    private final BuildCompatibilityService compatibilityService;

    public BudgetFeasibilityServiceImpl(ProductMapper productMapper, BuildCompatibilityService compatibilityService) {
        this.productMapper = productMapper;
        this.compatibilityService = compatibilityService;
    }

    @Override
    public Optional<BudgetFeasibilityResult> findForMessage(String message) {
        BigDecimal budget = extractBudget(message);
        if (budget == null) return Optional.empty();
        return findForBudget(budget);
    }

    @Override
    public Optional<BudgetFeasibilityResult> findForBudget(BigDecimal budget) {
        if (budget == null) return Optional.empty();
        return Optional.of(calculateForBudget(budget));
    }

    private BudgetFeasibilityResult calculateForBudget(BigDecimal budget) {
        Map<String, List<Product>> products = productsBySlot();
        if (products.size() != SLOT_TYPES.size() || products.values().stream().anyMatch(List::isEmpty)) {
            return new BudgetFeasibilityResult(budget, false, null, null, null);
        }

        BudgetBuildCandidate minimum = findMinimum(products);
        if (minimum == null) return new BudgetFeasibilityResult(budget, false, null, null, null);
        BigDecimal gap = minimum.totalPrice().subtract(budget).max(BigDecimal.ZERO);
        if (gap.signum() > 0) {
            return new BudgetFeasibilityResult(budget, false, minimum.totalPrice(), gap, null);
        }
        return new BudgetFeasibilityResult(budget, true, minimum.totalPrice(), BigDecimal.ZERO, minimum);
    }

    private Map<String, List<Product>> productsBySlot() {
        List<Product> active = productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
        Map<String, List<Product>> grouped = new LinkedHashMap<>();
        for (String type : SLOT_TYPES) {
            List<Product> products = active.stream().filter(product -> type.equals(product.getProductType()))
                    .sorted(Comparator.comparing(Product::getPrice).thenComparing(Product::getId)).toList();
            grouped.put(type, products);
        }
        return grouped;
    }

    private BudgetBuildCandidate findMinimum(Map<String, List<Product>> products) {
        List<List<Product>> options = SLOT_TYPES.stream().map(products::get).toList();
        PriorityQueue<List<Integer>> candidates = new PriorityQueue<>((left, right) -> compareIndexes(left, right, options));
        Set<String> visited = new HashSet<>();
        List<Integer> first = List.of(0, 0, 0, 0, 0, 0, 0);
        candidates.add(first);
        visited.add(first.toString());

        while (!candidates.isEmpty()) {
            List<Integer> indexes = candidates.remove();
            Product cpu = options.get(0).get(indexes.get(0));
            Product motherboard = options.get(1).get(indexes.get(1));
            Product gpu = options.get(2).get(indexes.get(2));
            Product ram = options.get(3).get(indexes.get(3));
            Product ssd = options.get(4).get(indexes.get(4));
            Product psu = options.get(5).get(indexes.get(5));
            Product computerCase = options.get(6).get(indexes.get(6));
            BuildCheckRequest request = new BuildCheckRequest(cpu.getId(), motherboard.getId(), gpu.getId(), ram.getId(), ssd.getId(), psu.getId(), computerCase.getId());
            BuildCheckResult check = compatibilityService.check(request);
            if ("PASS".equals(check.status())) {
                List<Product> selected = List.of(cpu, motherboard, gpu, ram, ssd, psu, computerCase);
                BigDecimal total = selected.stream().map(Product::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
                return new BudgetBuildCandidate(request, selected, total, check);
            }
            for (int slotIndex = 0; slotIndex < SLOT_TYPES.size(); slotIndex++) {
                int nextIndex = indexes.get(slotIndex) + 1;
                if (nextIndex >= options.get(slotIndex).size()) continue;
                List<Integer> next = new ArrayList<>(indexes);
                next.set(slotIndex, nextIndex);
                if (visited.add(next.toString())) candidates.add(next);
            }
        }
        return null;
    }

    private int compareIndexes(List<Integer> first, List<Integer> second, List<List<Product>> options) {
        int price = totalPrice(first, options).compareTo(totalPrice(second, options));
        if (price != 0) return price;
        for (int index = 0; index < first.size(); index++) {
            int id = Long.compare(options.get(index).get(first.get(index)).getId(), options.get(index).get(second.get(index)).getId());
            if (id != 0) return id;
        }
        return 0;
    }

    private BigDecimal totalPrice(List<Integer> indexes, List<List<Product>> options) {
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < indexes.size(); index++) total = total.add(options.get(index).get(indexes.get(index)).getPrice());
        return total;
    }

    private BigDecimal extractBudget(String message) {
        if (message == null || !EXPLICIT_BUILD_INTENT.matcher(message).find()) return null;
        Matcher amount = AMOUNT.matcher(message);
        if (!amount.find()) return null;
        boolean startsWithAmount = message.trim().startsWith(amount.group());
        if (!startsWithAmount && !BUDGET_CUE.matcher(message).find()) return null;
        return new BigDecimal(amount.group());
    }
}

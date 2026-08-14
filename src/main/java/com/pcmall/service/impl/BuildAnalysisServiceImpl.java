package com.pcmall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pcmall.client.DeepSeekClient;
import com.pcmall.client.DeepSeekCompletion;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.BuildAnalysisService;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BuildAnalysisResult;
import com.pcmall.vo.BuildCheckIssue;
import com.pcmall.vo.BuildCheckResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
public class BuildAnalysisServiceImpl implements BuildAnalysisService {
    private static final List<String> SLOT_TYPES = List.of("CPU", "MOTHERBOARD", "GPU", "RAM", "SSD", "PSU", "CASE");
    private static final Pattern UNSUPPORTED_COMPATIBILITY_CLAIM = Pattern.compile(
            "(可以|可|能够|能).{0,12}(装机|使用|运行|搭配|稳定|点亮)|无法(装机|使用|运行|搭配|稳定|点亮)|不需要更换|无需(更换|调整)|允许"
    );
    private static final Pattern UNSUPPORTED_STATUS_CLAIM = Pattern.compile(
            "(?i)\\b(PASS|WARNING|ERROR)\\b|完全兼容|检查通过|兼容通过|通过检查|无兼容性问题"
    );
    private static final String SYSTEM_PROMPT = "你是面向小白的电脑装机顾问。兼容性结论只能解释输入中的后端检查结果，不得自行推断或改变结论，不得自行判断硬件能否运行、是否可以使用、是否稳定或是否点亮。不得写出 PASS、WARNING、ERROR、完全兼容、检查通过等状态结论；请统一使用“后端检查提示”描述。只能提及输入中的当前商品和候选商品，不得编造型号、价格、规格或其他商品。出现错误时先解释错误和替换方向；出现电源余量提示时只说明这是后端估算建议；没有问题时说明适用场景和升级取舍。使用简洁普通文本输出，不得使用 Markdown 标题、加粗、表格、分隔线或 Emoji 标题。";
    private final BuildCompatibilityService compatibilityService;
    private final ProductMapper productMapper;
    private final DeepSeekClient deepSeekClient;
    private final ObjectMapper objectMapper;

    public BuildAnalysisServiceImpl(BuildCompatibilityService compatibilityService, ProductMapper productMapper,
                                    DeepSeekClient deepSeekClient, ObjectMapper objectMapper) {
        this.compatibilityService = compatibilityService;
        this.productMapper = productMapper;
        this.deepSeekClient = deepSeekClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public BuildAnalysisResult analyze(BuildCheckRequest request) {
        BuildCheckResult check = compatibilityService.check(request);
        if ("INCOMPLETE".equals(check.status())) return new BuildAnalysisResult(check, null, List.of(), false);
        Map<String, Product> selected = resolveSelectedProducts(request);
        List<Product> candidates = findCandidates(request, check, selected);
        try {
            DeepSeekCompletion completion = deepSeekClient.completePlain(List.of(message("system", buildPrompt(check, selected, candidates))));
            if (completion.hasToolCall() || completion.content() == null || completion.content().isBlank()) {
                return new BuildAnalysisResult(check, null, candidates, true);
            }
            String reply = sanitizeReply(completion.content());
            if (reply.isBlank()) {
                return new BuildAnalysisResult(check, null, candidates, true);
            }
            return new BuildAnalysisResult(check, reply, candidates, false);
        } catch (RuntimeException exception) {
            return new BuildAnalysisResult(check, null, candidates, true);
        }
    }

    private Map<String, Product> resolveSelectedProducts(BuildCheckRequest request) {
        Map<String, Long> ids = slotIds(request);
        Map<Long, Product> products = productMapper.selectBatchIds(ids.values()).stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
        Map<String, Product> selected = new LinkedHashMap<>();
        for (String slot : SLOT_TYPES) {
            Product product = products.get(ids.get(slot));
            if (product == null || !Integer.valueOf(1).equals(product.getStatus()) || !slot.equals(product.getProductType())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "槽位 " + slot + " 的商品不存在、已下架或类型不正确");
            }
            selected.put(slot, product);
        }
        return selected;
    }

    private List<Product> findCandidates(BuildCheckRequest request, BuildCheckResult initial, Map<String, Product> selected) {
        if ("PASS".equals(initial.status())) return List.of();
        Set<String> initialErrors = initial.issues().stream().filter(issue -> "ERROR".equals(issue.level())).map(BuildCheckIssue::rule).collect(Collectors.toSet());
        List<Product> candidates = new ArrayList<>();
        Set<Long> seen = new LinkedHashSet<>();
        for (BuildCheckIssue issue : initial.issues()) {
            for (String slot : replacementSlots(issue.rule())) {
                if (candidates.size() >= 3) return List.copyOf(candidates);
                Product current = selected.get(slot);
                for (Product candidate : activeProducts(slot, current.getId())) {
                    BuildCheckResult simulated = compatibilityService.check(withReplacement(request, slot, candidate.getId()));
                    if (solves(issue, initialErrors, simulated) && seen.add(candidate.getId())) {
                        candidates.add(candidate);
                        break;
                    }
                }
            }
        }
        return List.copyOf(candidates);
    }

    private List<Product> activeProducts(String productType, Long currentId) {
        return productMapper.selectList(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
                        .eq(Product::getProductType, productType).ne(Product::getId, currentId).orderByAsc(Product::getPrice))
                .stream().filter(product -> productType.equals(product.getProductType())).limit(6).toList();
    }

    private boolean solves(BuildCheckIssue issue, Set<String> initialErrors, BuildCheckResult simulated) {
        Set<String> simulatedErrors = simulated.issues().stream().filter(item -> "ERROR".equals(item.level())).map(BuildCheckIssue::rule).collect(Collectors.toSet());
        boolean ruleResolved = simulated.issues().stream().noneMatch(item -> issue.rule().equals(item.rule()));
        return ruleResolved && simulatedErrors.stream().allMatch(initialErrors::contains);
    }

    private List<String> replacementSlots(String rule) {
        return switch (rule) {
            case "CPU_MOTHERBOARD_SOCKET" -> List.of("CPU", "MOTHERBOARD");
            case "MOTHERBOARD_RAM_MEMORY_TYPE" -> List.of("RAM", "MOTHERBOARD");
            case "GPU_CASE_LENGTH" -> List.of("GPU", "CASE");
            case "MOTHERBOARD_CASE_FORM_FACTOR" -> List.of("MOTHERBOARD", "CASE");
            case "SSD_MOTHERBOARD_INTERFACE" -> List.of("SSD", "MOTHERBOARD");
            case "PSU_POWER_HEADROOM" -> List.of("PSU");
            default -> List.of();
        };
    }

    private BuildCheckRequest withReplacement(BuildCheckRequest request, String slot, Long productId) {
        Map<String, Long> ids = new LinkedHashMap<>(slotIds(request));
        ids.put(slot, productId);
        return new BuildCheckRequest(ids.get("CPU"), ids.get("MOTHERBOARD"), ids.get("GPU"), ids.get("RAM"), ids.get("SSD"), ids.get("PSU"), ids.get("CASE"));
    }

    private Map<String, Long> slotIds(BuildCheckRequest request) {
        Map<String, Long> ids = new LinkedHashMap<>();
        ids.put("CPU", request.cpuId()); ids.put("MOTHERBOARD", request.motherboardId()); ids.put("GPU", request.gpuId());
        ids.put("RAM", request.ramId()); ids.put("SSD", request.ssdId()); ids.put("PSU", request.psuId()); ids.put("CASE", request.caseId());
        return ids;
    }

    private ObjectNode message(String role, String content) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("role", role); node.put("content", content);
        return node;
    }

    private String buildPrompt(BuildCheckResult check, Map<String, Product> selected, List<Product> candidates) {
        String current = selected.entrySet().stream().map(entry -> entry.getKey() + ": " + productFact(entry.getValue())).collect(Collectors.joining("\n"));
        String issues = check.issues().isEmpty() ? "无" : check.issues().stream().map(issue -> issue.level() + " " + issue.rule() + ": " + issue.message()).collect(Collectors.joining("\n"));
        String alternatives = candidates.isEmpty() ? "无候选商品" : candidates.stream().map(this::productFact).collect(Collectors.joining("\n"));
        return SYSTEM_PROMPT + "\n\n当前整机总价: " + money(check.totalPrice()) + "元\n检查状态: " + check.status()
                + "\n缺失槽位: " + String.join("、", check.missingSlots()) + "\n估算功耗: " + check.estimatedPowerW() + "W\n建议电源: " + check.recommendedPsuW()
                + "W\n检查问题:\n" + issues + "\n当前商品:\n" + current + "\n后端候选商品:\n" + alternatives;
    }

    private String productFact(Product product) {
        return product.getName() + "，品牌: " + product.getBrand() + "，价格: " + money(product.getPrice()) + "元，类型: " + product.getProductType() + "，规格: " + product.getTags();
    }

    private String money(BigDecimal price) { return price == null ? "未知" : price.stripTrailingZeros().toPlainString(); }

    static String sanitizeReply(String reply) {
        return String.join("\n", reply.replaceAll("(?m)^\\s*(#{1,6}\\s*|\\*{1,2}|---\\s*)", "").lines()
                .flatMap(line -> Arrays.stream(line.split("(?<=[。！？!?])\\s*")))
                .filter(sentence -> !sentence.isBlank())
                .filter(sentence -> UNSUPPORTED_COMPATIBILITY_CLAIM.matcher(sentence).find() == false)
                .filter(sentence -> UNSUPPORTED_STATUS_CLAIM.matcher(sentence).find() == false)
                .map(String::trim)
                .toList()).trim();
    }
}

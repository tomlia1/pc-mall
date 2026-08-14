package com.pcmall.service.impl;

import com.pcmall.common.BusinessException;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.entity.Product;
import com.pcmall.entity.ProductHardwareSpec;
import com.pcmall.mapper.ProductHardwareSpecMapper;
import com.pcmall.mapper.ProductMapper;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BuildCheckIssue;
import com.pcmall.vo.BuildCheckResult;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BuildCompatibilityServiceImpl implements BuildCompatibilityService {
    private static final int PLATFORM_POWER_W = 120;
    private static final BigDecimal POWER_HEADROOM = new BigDecimal("1.2");
    private final ProductMapper productMapper;
    private final ProductHardwareSpecMapper specMapper;

    public BuildCompatibilityServiceImpl(ProductMapper productMapper, ProductHardwareSpecMapper specMapper) {
        this.productMapper = productMapper;
        this.specMapper = specMapper;
    }

    @Override
    public BuildCheckResult check(BuildCheckRequest request) {
        LinkedHashMap<String, Long> slots = slots(request);
        Map<Long, Product> products = loadProducts(slots);
        validateProducts(slots, products);
        BigDecimal totalPrice = products.values().stream().map(Product::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<String> missingSlots = slots.entrySet().stream().filter(entry -> entry.getValue() == null).map(Map.Entry::getKey).toList();
        if (!missingSlots.isEmpty()) return new BuildCheckResult("INCOMPLETE", missingSlots, List.of(), totalPrice, null, null);

        Map<String, ProductHardwareSpec> specs = loadSpecs(slots, products);
        List<BuildCheckIssue> issues = new ArrayList<>();
        ProductHardwareSpec cpu = specs.get("CPU");
        ProductHardwareSpec motherboard = specs.get("MOTHERBOARD");
        ProductHardwareSpec gpu = specs.get("GPU");
        ProductHardwareSpec ram = specs.get("RAM");
        ProductHardwareSpec ssd = specs.get("SSD");
        ProductHardwareSpec psu = specs.get("PSU");
        ProductHardwareSpec computerCase = specs.get("CASE");

        requireSpecFields(issues, cpu, "CPU", "cpuSocket");
        requireSpecFields(issues, motherboard, "MOTHERBOARD", "cpuSocket", "memoryType", "formFactor", "supportedStorageInterfaces");
        requireSpecFields(issues, gpu, "GPU", "gpuLengthMm", "estimatedPowerW");
        requireSpecFields(issues, ram, "RAM", "memoryType");
        requireSpecFields(issues, ssd, "SSD", "storageInterface");
        requireSpecFields(issues, psu, "PSU", "psuWatt");
        requireSpecFields(issues, computerCase, "CASE", "supportedFormFactors", "caseGpuMaxLengthMm");

        if (issues.isEmpty()) {
            compareEqual(issues, "CPU_MOTHERBOARD_SOCKET", cpu.getCpuSocket(), motherboard.getCpuSocket(), "CPU 与主板插槽不一致。");
            compareEqual(issues, "MOTHERBOARD_RAM_MEMORY_TYPE", motherboard.getMemoryType(), ram.getMemoryType(), "主板与内存代际不一致。");
            if (gpu.getGpuLengthMm() > computerCase.getCaseGpuMaxLengthMm()) addError(issues, "GPU_CASE_LENGTH", "显卡长度 " + gpu.getGpuLengthMm() + "mm，超过机箱支持的 " + computerCase.getCaseGpuMaxLengthMm() + "mm。");
            if (!contains(computerCase.getSupportedFormFactors(), motherboard.getFormFactor())) addError(issues, "MOTHERBOARD_CASE_FORM_FACTOR", "主板板型 " + motherboard.getFormFactor() + " 不被机箱支持。");
            if (!contains(motherboard.getSupportedStorageInterfaces(), ssd.getStorageInterface())) addError(issues, "SSD_MOTHERBOARD_INTERFACE", "SSD 接口 " + ssd.getStorageInterface() + " 不被主板支持。");
        }

        Integer estimatedPowerW = null;
        Integer recommendedPsuW = null;
        if (cpu.getEstimatedPowerW() != null && gpu.getEstimatedPowerW() != null && psu.getPsuWatt() != null) {
            estimatedPowerW = cpu.getEstimatedPowerW() + gpu.getEstimatedPowerW() + PLATFORM_POWER_W;
            recommendedPsuW = BigDecimal.valueOf(estimatedPowerW).multiply(POWER_HEADROOM).setScale(0, RoundingMode.CEILING).intValueExact();
            if (psu.getPsuWatt() < recommendedPsuW) {
                issues.add(new BuildCheckIssue("WARNING", "PSU_POWER_HEADROOM", "当前电源 " + psu.getPsuWatt() + "W，估算建议至少 " + recommendedPsuW + "W；这是基于部件功耗和平台预留的估算建议，不代表实测。"));
            }
        }

        boolean hasError = issues.stream().anyMatch(issue -> "ERROR".equals(issue.level()));
        boolean hasWarning = issues.stream().anyMatch(issue -> "WARNING".equals(issue.level()));
        return new BuildCheckResult(hasError ? "ERROR" : hasWarning ? "WARNING" : "PASS", List.of(), issues, totalPrice, estimatedPowerW, recommendedPsuW);
    }

    private LinkedHashMap<String, Long> slots(BuildCheckRequest request) {
        LinkedHashMap<String, Long> slots = new LinkedHashMap<>();
        slots.put("CPU", request.cpuId()); slots.put("MOTHERBOARD", request.motherboardId()); slots.put("GPU", request.gpuId());
        slots.put("RAM", request.ramId()); slots.put("SSD", request.ssdId()); slots.put("PSU", request.psuId()); slots.put("CASE", request.caseId());
        return slots;
    }

    private Map<Long, Product> loadProducts(Map<String, Long> slots) {
        List<Long> ids = slots.values().stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        return productMapper.selectBatchIds(ids).stream().collect(Collectors.toMap(Product::getId, product -> product));
    }

    private void validateProducts(Map<String, Long> slots, Map<Long, Product> products) {
        for (Map.Entry<String, Long> entry : slots.entrySet()) {
            if (entry.getValue() == null) continue;
            Product product = products.get(entry.getValue());
            if (product == null || !Integer.valueOf(1).equals(product.getStatus()) || !entry.getKey().equals(product.getProductType())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "槽位 " + entry.getKey() + " 的商品不存在、已下架或类型不正确");
            }
        }
    }

    private Map<String, ProductHardwareSpec> loadSpecs(Map<String, Long> slots, Map<Long, Product> products) {
        Map<Long, ProductHardwareSpec> specsById = specMapper.selectBatchIds(products.keySet()).stream()
                .collect(Collectors.toMap(ProductHardwareSpec::getProductId, spec -> spec));
        Map<String, ProductHardwareSpec> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : slots.entrySet()) result.put(entry.getKey(), specsById.get(entry.getValue()));
        return result;
    }

    private void requireSpecFields(List<BuildCheckIssue> issues, ProductHardwareSpec spec, String slot, String... fields) {
        if (spec == null) { addError(issues, "HARDWARE_SPEC_MISSING", slot + " 缺少基础规格，无法完成检查。"); return; }
        for (String field : fields) {
            boolean missing = switch (field) {
                case "cpuSocket" -> !StringUtils.hasText(spec.getCpuSocket());
                case "memoryType" -> !StringUtils.hasText(spec.getMemoryType());
                case "formFactor" -> !StringUtils.hasText(spec.getFormFactor());
                case "supportedFormFactors" -> !StringUtils.hasText(spec.getSupportedFormFactors());
                case "gpuLengthMm" -> spec.getGpuLengthMm() == null;
                case "caseGpuMaxLengthMm" -> spec.getCaseGpuMaxLengthMm() == null;
                case "storageInterface" -> !StringUtils.hasText(spec.getStorageInterface());
                case "supportedStorageInterfaces" -> !StringUtils.hasText(spec.getSupportedStorageInterfaces());
                case "psuWatt" -> spec.getPsuWatt() == null;
                case "estimatedPowerW" -> spec.getEstimatedPowerW() == null;
                default -> true;
            };
            if (missing) addError(issues, "HARDWARE_SPEC_MISSING", slot + " 缺少 " + field + "，无法完成检查。");
        }
    }

    private void compareEqual(List<BuildCheckIssue> issues, String rule, String first, String second, String message) {
        if (!first.equalsIgnoreCase(second)) addError(issues, rule, message);
    }

    private boolean contains(String values, String expected) {
        return Arrays.stream(values.split(",")).map(String::trim).anyMatch(value -> value.equalsIgnoreCase(expected));
    }

    private void addError(List<BuildCheckIssue> issues, String rule, String message) { issues.add(new BuildCheckIssue("ERROR", rule, message)); }
}

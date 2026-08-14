package com.pcmall.service.impl;

import com.pcmall.entity.Product;
import com.pcmall.vo.BuildAnalysisResult;
import com.pcmall.vo.BuildCheckIssue;
import com.pcmall.vo.BuildCheckResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildAnalysisStreamServiceImplTest {

    @Test
    void incompleteBuildSendsCheckThenCompletionPromptWithoutProducts() {
        BuildCheckResult check = new BuildCheckResult("INCOMPLETE", List.of("GPU"), List.of(), BigDecimal.ZERO, null, null);
        BuildAnalysisResult result = new BuildAnalysisResult(check, null, List.of(), false);

        var events = BuildAnalysisStreamServiceImpl.events(result);

        assertEquals(List.of("check", "error", "done"), events.stream().map(BuildAnalysisEvent::name).toList());
        assertEquals("请先完成基础选配后再请求 AI 解读。", ((java.util.Map<?, ?>) events.get(1).data()).get("message"));
    }

    @Test
    void unavailableAiKeepsCheckAndCandidateProductBeforeFriendlyError() {
        Product candidate = new Product(); candidate.setId(8L); candidate.setName("候选电源");
        BuildCheckResult check = new BuildCheckResult("WARNING", List.of(), List.of(new BuildCheckIssue("WARNING", "PSU_POWER_HEADROOM", "估算建议。")), BigDecimal.ONE, 383, 460);
        BuildAnalysisResult result = new BuildAnalysisResult(check, null, List.of(candidate), true);

        var events = BuildAnalysisStreamServiceImpl.events(result);

        assertEquals(List.of("check", "product", "error", "done"), events.stream().map(BuildAnalysisEvent::name).toList());
        assertEquals(candidate, events.get(1).data());
        assertEquals("AI 解读暂时不可用，请稍后重试", ((java.util.Map<?, ?>) events.get(2).data()).get("message"));
    }
}

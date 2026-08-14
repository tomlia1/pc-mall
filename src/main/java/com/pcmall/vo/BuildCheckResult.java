package com.pcmall.vo;

import java.math.BigDecimal;
import java.util.List;

public record BuildCheckResult(String status, List<String> missingSlots, List<BuildCheckIssue> issues,
                               BigDecimal totalPrice, Integer estimatedPowerW, Integer recommendedPsuW) { }

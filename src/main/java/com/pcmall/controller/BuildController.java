package com.pcmall.controller;

import com.pcmall.common.Result;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.service.BuildCompatibilityService;
import com.pcmall.vo.BuildCheckResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/builds")
public class BuildController {
    private final BuildCompatibilityService buildCompatibilityService;
    public BuildController(BuildCompatibilityService buildCompatibilityService) { this.buildCompatibilityService = buildCompatibilityService; }
    @PostMapping("/check")
    public Result<BuildCheckResult> check(@Valid @RequestBody BuildCheckRequest request) {
        return Result.success(buildCompatibilityService.check(request));
    }
}

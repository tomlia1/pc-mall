package com.pcmall.controller;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.security.AiRequestGuard;
import com.pcmall.service.BuildAnalysisStreamService;
import com.pcmall.util.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/builds")
public class BuildAnalysisController {
    private final BuildAnalysisStreamService buildAnalysisStreamService;
    private final AiRequestGuard aiRequestGuard;

    public BuildAnalysisController(BuildAnalysisStreamService buildAnalysisStreamService, AiRequestGuard aiRequestGuard) {
        this.buildAnalysisStreamService = buildAnalysisStreamService;
        this.aiRequestGuard = aiRequestGuard;
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(@Valid @RequestBody BuildCheckRequest request, HttpServletRequest httpRequest) {
        aiRequestGuard.acquire(ClientIpResolver.resolve(httpRequest));
        try {
            return buildAnalysisStreamService.stream(request, aiRequestGuard::release);
        } catch (RuntimeException exception) {
            aiRequestGuard.release();
            throw exception;
        }
    }
}

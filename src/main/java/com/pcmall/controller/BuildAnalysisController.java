package com.pcmall.controller;

import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.service.BuildAnalysisStreamService;
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

    public BuildAnalysisController(BuildAnalysisStreamService buildAnalysisStreamService) {
        this.buildAnalysisStreamService = buildAnalysisStreamService;
    }

    @PostMapping(value = "/analyze/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter analyze(@Valid @RequestBody BuildCheckRequest request) {
        return buildAnalysisStreamService.stream(request);
    }
}

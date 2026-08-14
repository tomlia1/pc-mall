package com.pcmall.service;

import com.pcmall.dto.BuildCheckRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface BuildAnalysisStreamService {
    SseEmitter stream(BuildCheckRequest request);
}

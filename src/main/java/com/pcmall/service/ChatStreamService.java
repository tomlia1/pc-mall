package com.pcmall.service;

import com.pcmall.dto.ChatRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatStreamService {
    SseEmitter stream(ChatRequest request);
}

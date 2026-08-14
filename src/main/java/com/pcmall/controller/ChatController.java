package com.pcmall.controller;

import com.pcmall.dto.ChatRequest;
import com.pcmall.security.AiRequestGuard;
import com.pcmall.service.ChatStreamService;
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
@RequestMapping("/api/v1/chat")
public class ChatController {
    private final ChatStreamService chatStreamService;
    private final AiRequestGuard aiRequestGuard;

    public ChatController(ChatStreamService chatStreamService, AiRequestGuard aiRequestGuard) {
        this.chatStreamService = chatStreamService;
        this.aiRequestGuard = aiRequestGuard;
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        aiRequestGuard.acquire(ClientIpResolver.resolve(httpRequest));
        try {
            return chatStreamService.stream(request, aiRequestGuard::release);
        } catch (RuntimeException exception) {
            aiRequestGuard.release();
            throw exception;
        }
    }
}

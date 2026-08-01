package com.pcmall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.ChatRequest;
import com.pcmall.entity.Product;
import com.pcmall.service.ChatService;
import com.pcmall.service.ChatStreamService;
import com.pcmall.vo.ChatResult;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ChatStreamServiceImpl implements ChatStreamService {
    private static final long STREAM_TIMEOUT_MILLIS = 30_000L;
    private final ChatService chatService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatStreamServiceImpl(ChatService chatService, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicReference<Future<?>> taskReference = new AtomicReference<>();
        AtomicBoolean finished = new AtomicBoolean(false);
        Runnable cancel = () -> cancelIfPending(finished, taskReference.get());
        emitter.onCompletion(cancel);
        emitter.onTimeout(() -> {
            cancel.run();
            emitter.complete();
        });
        taskReference.set(executor.submit(() -> produceEvents(request, emitter, finished)));
        return emitter;
    }

    private void produceEvents(ChatRequest request, SseEmitter emitter, AtomicBoolean finished) {
        try {
            ChatResult result = chatService.chat(request);
            for (String part : splitText(result.reply(), 16)) send(emitter, "message", Map.of("text", part));
            for (Product product : result.products()) send(emitter, "product", product);
            send(emitter, "done", Map.of());
            finished.set(true);
            emitter.complete();
        } catch (IOException ignored) {
            emitter.complete();
        } catch (BusinessException exception) {
            sendError(emitter, exception.getMessage());
        } catch (Exception ignored) {
            sendError(emitter, "推荐服务暂时不可用，请稍后再试");
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", Map.of("message", message));
            send(emitter, "done", Map.of());
        } catch (IOException ignored) {
            // The client has disconnected; no response body can be delivered.
        } finally {
            emitter.complete();
        }
    }

    private void send(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(data)));
    }

    static List<String> splitText(String text, int maxCodePoints) {
        List<String> parts = new ArrayList<>();
        if (text == null || text.isEmpty()) return parts;
        int start = 0;
        while (start < text.length()) {
            int remaining = text.codePointCount(start, text.length());
            int end = text.offsetByCodePoints(start, Math.min(maxCodePoints, remaining));
            parts.add(text.substring(start, end));
            start = end;
        }
        return parts;
    }

    static void cancelIfPending(AtomicBoolean finished, Future<?> task) {
        if (!finished.get() && task != null) task.cancel(true);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}

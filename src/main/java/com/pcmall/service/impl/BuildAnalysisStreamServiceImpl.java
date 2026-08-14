package com.pcmall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcmall.common.BusinessException;
import com.pcmall.dto.BuildCheckRequest;
import com.pcmall.service.BuildAnalysisService;
import com.pcmall.service.BuildAnalysisStreamService;
import com.pcmall.vo.BuildAnalysisResult;
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
public class BuildAnalysisStreamServiceImpl implements BuildAnalysisStreamService {
    private static final long STREAM_TIMEOUT_MILLIS = 30_000L;
    private final BuildAnalysisService buildAnalysisService;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public BuildAnalysisStreamServiceImpl(BuildAnalysisService buildAnalysisService, ObjectMapper objectMapper) {
        this.buildAnalysisService = buildAnalysisService;
        this.objectMapper = objectMapper;
    }

    @Override
    public SseEmitter stream(BuildCheckRequest request) {
        return stream(request, () -> { });
    }

    @Override
    public SseEmitter stream(BuildCheckRequest request, Runnable completionCallback) {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MILLIS);
        AtomicReference<Future<?>> taskReference = new AtomicReference<>();
        AtomicBoolean finished = new AtomicBoolean(false);
        AtomicBoolean completionNotified = new AtomicBoolean(false);
        Runnable complete = () -> {
            if (!finished.get() && taskReference.get() != null) taskReference.get().cancel(true);
            if (completionNotified.compareAndSet(false, true)) completionCallback.run();
        };
        emitter.onCompletion(complete);
        emitter.onTimeout(() -> { complete.run(); emitter.complete(); });
        emitter.onError(error -> complete.run());
        taskReference.set(executor.submit(() -> produceEvents(request, emitter, finished, complete)));
        return emitter;
    }

    static List<BuildAnalysisEvent> events(BuildAnalysisResult result) {
        List<BuildAnalysisEvent> events = new ArrayList<>();
        events.add(new BuildAnalysisEvent("check", result.check()));
        result.candidates().forEach(product -> events.add(new BuildAnalysisEvent("product", product)));
        if ("INCOMPLETE".equals(result.check().status())) {
            events.add(new BuildAnalysisEvent("error", Map.of("message", "请先完成基础选配后再请求 AI 解读。")));
        } else if (result.aiUnavailable()) {
            events.add(new BuildAnalysisEvent("error", Map.of("message", "AI 解读暂时不可用，请稍后重试")));
        } else if (result.reply() != null && !result.reply().isBlank()) {
            events.add(new BuildAnalysisEvent("message", Map.of("text", result.reply())));
        }
        events.add(new BuildAnalysisEvent("done", Map.of()));
        return events;
    }

    private void produceEvents(BuildCheckRequest request, SseEmitter emitter, AtomicBoolean finished, Runnable complete) {
        try {
            BuildAnalysisResult result = buildAnalysisService.analyze(request);
            for (BuildAnalysisEvent event : events(result)) send(emitter, event.name(), event.data());
            finished.set(true);
            emitter.complete();
        } catch (BusinessException exception) {
            sendError(emitter, exception.getMessage());
        } catch (Exception exception) {
            sendError(emitter, "AI 解读暂时不可用，请稍后重试");
        } finally {
            finished.set(true);
            complete.run();
        }
    }

    private void sendError(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", Map.of("message", message));
            send(emitter, "done", Map.of());
        } catch (IOException ignored) {
            // The browser disconnected before the stream could be completed.
        } finally {
            emitter.complete();
        }
    }

    private void send(SseEmitter emitter, String name, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(name).data(objectMapper.writeValueAsString(data)));
    }

    @PreDestroy
    void shutdown() { executor.shutdownNow(); }
}

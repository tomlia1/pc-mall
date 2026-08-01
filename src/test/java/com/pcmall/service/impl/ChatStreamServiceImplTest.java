package com.pcmall.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcmall.dto.ChatRequest;
import com.pcmall.service.ChatService;
import com.pcmall.vo.ChatResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatStreamServiceImplTest {
    private ChatStreamServiceImpl service;

    @AfterEach
    void tearDown() {
        if (service != null) service.shutdown();
    }

    @Test
    void delegatesToChatServiceAndProducesSseEmitter() throws Exception {
        ChatService chatService = mock(ChatService.class);
        CountDownLatch called = new CountDownLatch(1);
        when(chatService.chat(any())).thenAnswer(invocation -> {
            called.countDown();
            return new ChatResult("推荐这台电脑", List.of());
        });
        service = new ChatStreamServiceImpl(chatService, new ObjectMapper());

        SseEmitter emitter = service.stream(new ChatRequest("550e8400-e29b-41d4-a716-446655440000", "预算五千"));

        assertEquals(30000L, emitter.getTimeout());
        assertTrue(called.await(2, TimeUnit.SECONDS));
        verify(chatService).chat(any());
    }

    @Test
    void splitsTextByCodePointsWithoutBreakingChineseCharacters() {
        assertEquals(List.of("推荐你", "😀这台", "电脑"), ChatStreamServiceImpl.splitText("推荐你😀这台电脑", 3));
    }

    @Test
    void cancelsPendingTaskWhenAClientDisconnects() {
        Future<?> task = mock(Future.class);

        ChatStreamServiceImpl.cancelIfPending(new java.util.concurrent.atomic.AtomicBoolean(false), task);

        verify(task).cancel(true);
    }
}

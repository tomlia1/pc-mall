package com.pcmall.security;

import com.pcmall.common.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiRequestGuardTest {
    @Test
    void rejectsTheFifthConcurrentAiRequest() {
        AiRequestGuard guard = new AiRequestGuard(new RequestRateLimiter());

        assertDoesNotThrow(() -> guard.acquire("203.0.113.10"));
        assertDoesNotThrow(() -> guard.acquire("203.0.113.11"));
        assertDoesNotThrow(() -> guard.acquire("203.0.113.12"));
        assertDoesNotThrow(() -> guard.acquire("203.0.113.13"));
        assertThrows(BusinessException.class, () -> guard.acquire("203.0.113.14"));
    }

    @Test
    void releasesConcurrencySlotWhenStreamCompletes() {
        AiRequestGuard guard = new AiRequestGuard(new RequestRateLimiter());

        for (int index = 0; index < 4; index++) guard.acquire("203.0.113." + index);
        guard.release();

        assertDoesNotThrow(() -> guard.acquire("203.0.113.20"));
    }

    @Test
    void rejectsEleventhRequestFromSameIpWithinOneMinute() {
        AiRequestGuard guard = new AiRequestGuard(new RequestRateLimiter());

        for (int index = 0; index < 10; index++) {
            guard.acquire("203.0.113.30");
            guard.release();
        }

        assertThrows(BusinessException.class, () -> guard.acquire("203.0.113.30"));
    }
}

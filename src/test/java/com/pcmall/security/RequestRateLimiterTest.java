package com.pcmall.security;

import org.junit.jupiter.api.Test;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class RequestRateLimiterTest {
    @Test
    void rejectsFourthRequestInsideThreeRequestWindow() {
        RequestRateLimiter limiter = new RequestRateLimiter();
        assertTrue(limiter.tryAcquire("lead:127.0.0.1", 3, Duration.ofDays(1)));
        assertTrue(limiter.tryAcquire("lead:127.0.0.1", 3, Duration.ofDays(1)));
        assertTrue(limiter.tryAcquire("lead:127.0.0.1", 3, Duration.ofDays(1)));
        assertFalse(limiter.tryAcquire("lead:127.0.0.1", 3, Duration.ofDays(1)));
    }
}

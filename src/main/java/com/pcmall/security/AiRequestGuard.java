package com.pcmall.security;

import com.pcmall.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Semaphore;

@Component
public class AiRequestGuard {
    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int MAX_CONCURRENT_REQUESTS = 4;

    private final RequestRateLimiter rateLimiter;
    private final Semaphore concurrentRequests = new Semaphore(MAX_CONCURRENT_REQUESTS);

    public AiRequestGuard(RequestRateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    public void acquire(String clientIp) {
        if (!rateLimiter.tryAcquire("ai:" + clientIp, REQUESTS_PER_MINUTE, Duration.ofMinutes(1))) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "AI 请求过于频繁，请稍后再试");
        }
        if (!concurrentRequests.tryAcquire()) {
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "当前 AI 请求较多，请稍后再试");
        }
    }

    public void release() {
        concurrentRequests.release();
    }
}

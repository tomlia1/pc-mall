package com.pcmall.security;

import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RequestRateLimiter {
    private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();
    public boolean tryAcquire(String key, int limit, Duration window) {
        Instant now = Instant.now();
        Counter counter = counters.compute(key, (ignored, previous) -> {
            Counter current = previous == null || !now.isBefore(previous.startedAt.plus(window)) ? new Counter(now) : previous;
            current.count.incrementAndGet();
            return current;
        });
        return counter.count.get() <= limit;
    }
    private static final class Counter {
        private final Instant startedAt; private final AtomicInteger count = new AtomicInteger();
        private Counter(Instant startedAt) { this.startedAt = startedAt; }
    }
}

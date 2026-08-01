package com.pcmall.security;

import com.pcmall.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptGuard {
    private static final int MAX_FAILURES = 5;
    private static final Duration FAILURE_WINDOW = Duration.ofMinutes(5);
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    public void check(String ip) {
        Attempt attempt = attempts.get(ip);
        if (attempt != null && attempt.lockedUntil != null && Instant.now().isBefore(attempt.lockedUntil))
            throw new BusinessException(HttpStatus.TOO_MANY_REQUESTS, "登录尝试过多，请稍后再试");
    }
    public void failure(String ip) {
        attempts.compute(ip, (key, old) -> {
            Instant now = Instant.now();
            Attempt current = old == null || now.isAfter(old.windowStarted.plus(FAILURE_WINDOW)) ? new Attempt(now, 0, null) : old;
            int failures = current.failures + 1;
            return new Attempt(current.windowStarted, failures, failures >= MAX_FAILURES ? now.plus(LOCK_DURATION) : current.lockedUntil);
        });
    }
    public void success(String ip) { attempts.remove(ip); }
    private record Attempt(Instant windowStarted, int failures, Instant lockedUntil) { }
}

package com.dnxt.globaladmin.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory rate limiter for login endpoint.
 * Tracks login attempts per IP address with a sliding window.
 */
@Component
public class LoginRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(LoginRateLimiter.class);

    @Value("${admin.security.login-rate-limit-per-ip:10}")
    private int maxAttemptsPerWindow;

    @Value("${admin.security.login-rate-limit-window-minutes:15}")
    private int windowMinutes;

    private final Map<String, WindowEntry> attempts = new ConcurrentHashMap<>();

    public boolean isRateLimited(String ipAddress) {
        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60_000L;

        WindowEntry entry = attempts.compute(ipAddress, (key, existing) -> {
            if (existing == null || (now - existing.windowStart) > windowMs) {
                return new WindowEntry(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        boolean limited = entry.count.get() > maxAttemptsPerWindow;
        if (limited) {
            log.warn("Rate limit exceeded for IP: {} ({} attempts in {} min window)",
                    ipAddress, entry.count.get(), windowMinutes);
        }
        return limited;
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        long windowMs = windowMinutes * 60_000L;
        attempts.entrySet().removeIf(e -> (now - e.getValue().windowStart) > windowMs);
    }

    private static class WindowEntry {
        final long windowStart;
        final AtomicInteger count;

        WindowEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}

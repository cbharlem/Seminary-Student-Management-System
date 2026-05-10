package com.seminary.sms.config;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * IP-based rate limiter for public unauthenticated endpoints.
 * Limits each IP to MAX_SUBMISSIONS attempts per WINDOW_MS (default: 3 per hour).
 * Uses the same in-memory ConcurrentHashMap pattern as LoginAttemptService.
 */
@Service
public class IpRateLimitService {

    private static final int  MAX_SUBMISSIONS = 3;
    private static final long WINDOW_MS       = 60 * 60 * 1000; // 1 hour

    private final ConcurrentHashMap<String, AtomicInteger> counts    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>          windowStart = new ConcurrentHashMap<>();

    public boolean isAllowed(String ip) {
        long now = System.currentTimeMillis();

        // Reset counter if the window has expired for this IP
        Long start = windowStart.get(ip);
        if (start == null || now - start > WINDOW_MS) {
            counts.put(ip, new AtomicInteger(0));
            windowStart.put(ip, now);
        }

        int count = counts.get(ip).incrementAndGet();
        return count <= MAX_SUBMISSIONS;
    }
}

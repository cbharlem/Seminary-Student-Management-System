package com.seminary.sms.config;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * SECURITY (A07): Tracks failed login attempts per username.
 * Locks the account after 5 consecutive failures for 15 minutes.
 */
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000; // 15 minutes

    private final ConcurrentHashMap<String, AtomicInteger> attempts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> blockedUntil = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        // SECURITY (A07): Capture the count atomically to reduce TOCTOU window
        int count = attempts.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        if (count >= MAX_ATTEMPTS) {
            blockedUntil.put(username, System.currentTimeMillis() + BLOCK_DURATION_MS);
        }
    }

    public void loginSucceeded(String username) {
        attempts.remove(username);
        blockedUntil.remove(username);
    }

    public int getRemainingAttempts(String username) {
        AtomicInteger count = attempts.get(username);
        if (count == null) return MAX_ATTEMPTS;
        return Math.max(0, MAX_ATTEMPTS - count.get());
    }

    public boolean isBlocked(String username) {
        Long until = blockedUntil.get(username);
        if (until == null) return false;
        if (System.currentTimeMillis() > until) {
            // Block expired — clear it
            attempts.remove(username);
            blockedUntil.remove(username);
            return false;
        }
        return true;
    }
}

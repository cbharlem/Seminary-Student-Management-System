package com.seminary.sms.config;

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG / SUPPORT — LoginAttemptService
// This is not part of any numbered layer. It is a security support component
// that protects the login form against brute-force attacks.
//
// What it does:
//   Tracks how many consecutive failed login attempts each username has made.
//   After 5 failed attempts, the account is locked out for 15 minutes.
//   All tracking is done in memory (ConcurrentHashMap) — no database table needed.
//
// How it works:
//   loginFailed(username)         → increments the counter for that username.
//                                   If count reaches 5, records a lock-out timestamp.
//   loginSucceeded(username)      → clears the counter and any lock-out for that username.
//   isBlocked(username)           → returns true if the account is still locked.
//                                   Also auto-clears the lock if the 15-minute window has passed.
//   getRemainingAttempts(username)→ returns how many attempts are left before lockout.
//
// This service is used by SecurityConfig — it is called inside a custom filter
// that intercepts POST /login before Spring Security processes the credentials.
// ─────────────────────────────────────────────────────────────────────────────

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

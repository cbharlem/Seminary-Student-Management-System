package com.seminary.sms.config;

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG / SUPPORT — LoginAttemptService
// Tracks failed login attempts per username, locks after 5 failures for 15 min.
// On lock: looks up the user's email, generates a password-reset token, and
// sends a lock-notification email with a bypass link.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.User;
import com.seminary.sms.repository.UserRepository;
import com.seminary.sms.service.AuditService;
import com.seminary.sms.service.EmailService;
import com.seminary.sms.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final long BLOCK_DURATION_MS = 15 * 60 * 1000;

    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordResetService passwordResetService;
    @Autowired private AuditService auditService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    private final ConcurrentHashMap<String, AtomicInteger> attempts   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long>          blockedUntil = new ConcurrentHashMap<>();

    public void loginFailed(String username) {
        int count = attempts.computeIfAbsent(username, k -> new AtomicInteger(0)).incrementAndGet();
        if (count == MAX_ATTEMPTS) {
            blockedUntil.put(username, System.currentTimeMillis() + BLOCK_DURATION_MS);
            auditService.logSecurity(username, "ACCOUNT_LOCKED",
                "Account locked after " + MAX_ATTEMPTS + " failed login attempts");
            sendLockNotification(username);
        } else {
            auditService.logSecurity(username, "FAILED_LOGIN",
                "Failed login attempt " + count + " of " + MAX_ATTEMPTS);
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
            attempts.remove(username);
            blockedUntil.remove(username);
            return false;
        }
        return true;
    }

    private void sendLockNotification(String username) {
        Optional<User> opt = userRepository.findByUsername(username);
        if (opt.isEmpty()) return;
        User user = opt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) return;
        String token     = passwordResetService.generateToken(user);
        String resetLink = baseUrl + "/reset-password.html?token=" + token;
        emailService.sendAccountLocked(user.getEmail(), username, resetLink);
    }
}

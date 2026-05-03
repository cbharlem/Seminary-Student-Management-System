package com.seminary.sms.controller;

import com.seminary.sms.config.LoginAttemptService;
import com.seminary.sms.entity.PasswordResetToken;
import com.seminary.sms.entity.User;
import com.seminary.sms.repository.UserRepository;
import com.seminary.sms.service.AuditService;
import com.seminary.sms.service.EmailService;
import com.seminary.sms.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PasswordResetController {

    private final UserRepository        userRepository;
    private final PasswordResetService  passwordResetService;
    private final EmailService          emailService;
    private final LoginAttemptService   loginAttemptService;
    private final AuditService          auditService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // Accepts a username + email pair, verifies both match the same account, then sends a reset link.
    // Always returns 200 regardless of whether the pair matched to prevent username/email enumeration.
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "").trim();
        String email    = body.getOrDefault("email", "").trim();
        if (username.isBlank() || email.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Username and email are required."));

        Optional<User> opt = userRepository.findByUsername(username);
        // Both username AND email must match the same account before sending a link
        if (opt.isPresent() && email.equalsIgnoreCase(opt.get().getEmail())) {
            User user        = opt.get();
            String token     = passwordResetService.generateToken(user);
            String resetLink = baseUrl + "/reset-password.html?token=" + token;
            emailService.sendPasswordReset(email, user.getUsername(), resetLink);
            auditService.logSecurity(username, "PASSWORD_RESET_REQUESTED", "Password reset link sent to registered email");
        }
        return ResponseEntity.ok(Map.of("message", "If that username and email match an account, a reset link has been sent."));
    }

    // Validates a token — called by the reset-password page on load to detect expired/invalid links.
    @GetMapping("/validate-reset-token")
    public ResponseEntity<?> validateToken(@RequestParam String token) {
        boolean valid = passwordResetService.validateToken(token).isPresent();
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // Resets the password and clears the brute-force lock for the account.
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token       = body.getOrDefault("token", "").trim();
        String newPassword = body.getOrDefault("newPassword", "").trim();

        if (token.isBlank() || newPassword.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Token and new password are required."));

        if (newPassword.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters."));

        // Resolve the username before consuming the token (needed to clear the lock)
        Optional<PasswordResetToken> opt = passwordResetService.validateToken(token);
        if (opt.isEmpty()) {
            auditService.logSecurity("unknown", "INVALID_RESET_TOKEN", "Expired or already-used password reset link was submitted");
            return ResponseEntity.badRequest().body(Map.of("error", "This link is invalid or has expired."));
        }

        PasswordResetToken resetToken = opt.get();
        String username = resetToken.getUser().getUsername();

        // SECURITY (A07): Prevent password reuse
        if (passwordResetService.isSameAsCurrentPassword(resetToken, newPassword))
            return ResponseEntity.badRequest().body(Map.of("error", "New password must be different from your current password."));

        boolean success = passwordResetService.resetPassword(token, newPassword);
        if (!success)
            return ResponseEntity.badRequest().body(Map.of("error", "This link is invalid or has expired."));

        // Bypass the 15-minute lock — the user proved ownership via email
        loginAttemptService.loginSucceeded(username);
        auditService.logSecurity(username, "PASSWORD_RESET_COMPLETED", "Password successfully reset via email link");
        return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
    }
}

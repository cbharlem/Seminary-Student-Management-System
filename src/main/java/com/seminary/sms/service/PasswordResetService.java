package com.seminary.sms.service;

import com.seminary.sms.entity.PasswordResetToken;
import com.seminary.sms.entity.User;
import com.seminary.sms.repository.PasswordResetTokenRepository;
import com.seminary.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Generates a reset token for the given user and returns the raw token string.
    // Any previous tokens for this user are deleted first (one active token at a time).
    @Transactional
    public String generateToken(User user) {
        tokenRepository.deleteByUserIndex(user.getIndex());
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        PasswordResetToken token = PasswordResetToken.builder()
            .token(rawToken)
            .user(user)
            .expiresAt(LocalDateTime.now().plusHours(1))
            .used(false)
            .build();
        tokenRepository.save(token);
        return rawToken;
    }

    public boolean isSameAsCurrentPassword(PasswordResetToken token, String newPassword) {
        return passwordEncoder.matches(newPassword, token.getUser().getPasswordHash());
    }

    public Optional<PasswordResetToken> validateToken(String rawToken) {
        return tokenRepository.findByToken(rawToken).filter(t ->
            !t.getUsed() && t.getExpiresAt().isAfter(LocalDateTime.now())
        );
    }

    @Transactional
    public boolean resetPassword(String rawToken, String newPassword) {
        Optional<PasswordResetToken> opt = validateToken(rawToken);
        if (opt.isEmpty()) return false;
        PasswordResetToken token = opt.get();
        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        token.setUsed(true);
        tokenRepository.save(token);
        return true;
    }
}

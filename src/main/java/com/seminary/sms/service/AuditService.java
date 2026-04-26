package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (AuditService)
// Writes audit log entries when significant actions occur in the system.
// Called from controllers after successful write operations.
//
// Resolves the current user's username, role, and IP address automatically
// from Spring Security context and the HTTP request — callers only need to
// supply the action type, entity type, and a human-readable detail string.
//
// LAYER 3 → LAYER 4: Saves AuditLog entries via AuditLogRepository.
// LAYER 2 → LAYER 3: Controllers call log() after create/update/delete actions.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.AuditLog;
import com.seminary.sms.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final HttpServletRequest request;

    public void log(String action, String entityType, String detail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : "unknown";
        String role = (auth != null && !auth.getAuthorities().isEmpty())
            ? auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
            : "unknown";
        String ip = resolveClientIp();

        AuditLog entry = AuditLog.builder()
            .performedBy(username)
            .role(role)
            .action(action)
            .entityType(entityType)
            .detail(detail)
            .ipAddress(ip)
            .build();
        auditLogRepository.save(entry);
    }

    // Checks X-Forwarded-For header first (set by proxies) then falls back to remote address.
    private String resolveClientIp() {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

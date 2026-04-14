package com.seminary.sms.config;

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG / SUPPORT — GlobalExceptionHandler
// This is not part of any numbered layer. It is a cross-cutting support class
// that applies to the entire application.
//
// What it does:
//   Spring normally shows a default error page or exposes an exception message
//   when something goes wrong. That is dangerous — error messages can leak
//   internal class names, database details, or stack traces to attackers.
//
//   This class uses @RestControllerAdvice, which means Spring will automatically
//   call it whenever an unhandled exception escapes from any controller.
//   It acts as a safety net for the whole application.
//
// Exceptions handled:
//   AccessDeniedException    → returns HTTP 403 with a generic "Access denied." message
//   IllegalArgumentException → returns HTTP 400 with a generic message.
//                              Important: we do NOT expose e.getMessage() here because
//                              Java's Enum.valueOf() error messages include full class paths.
//   Exception (catch-all)    → returns HTTP 500 with a generic message.
//                              Prevents stack traces from ever reaching the browser.
// ─────────────────────────────────────────────────────────────────────────────

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * SECURITY (A05): Catches all unhandled exceptions so that stack traces,
 * internal class names, and database details are never exposed to the client.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(403).body(Map.of("error", "Access denied."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException e) {
        // SECURITY (A08): Never expose e.getMessage() for IllegalArgumentException —
        // Enum.valueOf() messages expose full class paths (e.g. com.seminary.sms.entity...)
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid request parameter."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        // SECURITY: Never expose e.getMessage() for unknown exceptions —
        // it may contain stack traces, class names, or DB details
        return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
    }
}

package com.seminary.sms.config;

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

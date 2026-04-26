package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (AuditLog)
// Maps to the database table: tblauditlog
// Records every significant action performed in the system — who did it,
// what entity was affected, and a human-readable description of the change.
// Only the Admin (Head of Seminary) can view the audit log.
//
// LAYER 5 → LAYER 4: AuditLogRepository queries tblauditlog using this entity.
// LAYER 5 → LAYER 2: AdminAuditController reads these to render the audit log page.
// LAYER 5 → LAYER 3: AuditService writes entries from all controllers.
// ─────────────────────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblauditlog")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldTimestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "fldPerformedBy", nullable = false, length = 50)
    private String performedBy;

    @Column(name = "fldRole", nullable = false, length = 20)
    private String role;

    @Column(name = "fldAction", nullable = false, length = 30)
    private String action;

    @Column(name = "fldEntityType", nullable = false, length = 50)
    private String entityType;

    @Column(name = "fldDetail", length = 500)
    private String detail;

    @Column(name = "fldIpAddress", length = 50)
    private String ipAddress;

    @PrePersist
    protected void onCreate() { timestamp = LocalDateTime.now(); }
}

package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (AuditLogRepository)
// Serves the AuditLog entity — reads from and writes to the tblauditlog table.
//
// LAYER 4 → LAYER 5: Uses the AuditLog entity to map database rows to objects.
// LAYER 4 → LAYER 3: AuditService writes entries via save().
// LAYER 4 → LAYER 2: AdminAuditController reads entries for the audit log page.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}

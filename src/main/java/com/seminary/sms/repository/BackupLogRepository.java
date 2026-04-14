package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (BackupLogRepository)
// Serves the BackupLog entity — reads from and writes to the tblbackuplog table.
//
// Spring auto-generates SQL from the method name declared here:
//   findAllByOrderByBackupDateDesc → retrieves all backup log entries, newest first
//                                    (the "OrderBy...Desc" part adds ORDER BY automatically)
//
// LAYER 4 → LAYER 5: Uses the BackupLog entity to map database rows to objects.
// LAYER 4 → LAYER 2: BackupController calls this repository to display the backup history.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BackupLogRepository extends JpaRepository<BackupLog, Integer> {

    // Auto-generates: SELECT * FROM tblbackuplog ORDER BY fldBackupDate DESC
    // Called by: BackupController.getLog() to show backup history newest-first
    List<BackupLog> findAllByOrderByBackupDateDesc();
}

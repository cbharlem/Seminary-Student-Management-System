package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (BackupLog)
// Maps to the database table: tblbackuplog
// Records each database backup that was performed — when it happened,
// what file was created, what type it was (Manual or Scheduled),
// and which User triggered it.
//
// Relationships:
//   @ManyToOne → User   (the admin who performed the backup)
//
// LAYER 5 → LAYER 4: BackupLogRepository queries tblbackuplog using this entity.
// LAYER 4 → LAYER 5: Queries return BackupLog objects.
// LAYER 5 → LAYER 2: BackupController reads these objects to show the backup history.
// ─────────────────────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblbackuplog")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BackupLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldBackupID", nullable = false, unique = true, length = 30)
    private String backupId;

    @Column(name = "fldBackupDate", nullable = false, updatable = false) private LocalDateTime backupDate;
    @Column(name = "fldBackupFilePath", nullable = false, length = 500)  private String backupFilePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldBackupType", nullable = false)
    @Builder.Default
    private BackupType backupType = BackupType.Manual;

    // Many backup log entries can be created by the same user — fldPerformedByIndex is the foreign key
    // FetchType.LAZY defers loading the User object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldPerformedByIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User performedBy;

    @Column(name = "fldNotes", length = 255) private String notes;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets backupDate to right now — this cannot be changed later (updatable = false on the column)
    @PrePersist
    protected void onCreate() { backupDate = LocalDateTime.now(); }

    public enum BackupType { Manual, Scheduled }
}

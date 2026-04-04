package com.seminary.sms.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldPerformedByIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User performedBy;

    @Column(name = "fldNotes", length = 255) private String notes;

    @PrePersist
    protected void onCreate() { backupDate = LocalDateTime.now(); }

    public enum BackupType { Manual, Scheduled }
}

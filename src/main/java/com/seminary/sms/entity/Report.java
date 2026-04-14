package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Report)
// Maps to the database table: tblreports
// Records each report that was generated in the system (e.g., Transcript of Records,
// Grade Card, CHED Report). Tracks who generated it, when, and in what format (PDF/XLSX).
//
// Relationships:
//   @ManyToOne → Student    (the student the report is about; optional for system-wide reports)
//   @ManyToOne → Semester   (the semester the report covers; optional)
//   @ManyToOne → User       (the registrar who generated the report)
//
// LAYER 5 → LAYER 4: ReportRepository queries tblreports using this entity.
// LAYER 4 → LAYER 5: Queries return Report objects.
// LAYER 5 → LAYER 2: Controllers use Report to log and retrieve generated report records.
// ─────────────────────────────────────────────────────────────────────────────

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblreports")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldReportID", nullable = false, unique = true, length = 30)
    private String reportId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldReportType", nullable = false)
    private ReportType reportType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldGeneratedByIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User generatedBy;

    @Column(name = "fldGeneratedAt", nullable = false, updatable = false) private LocalDateTime generatedAt;
    @Column(name = "fldFilePath", length = 500)                           private String filePath;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "fldExportFormat", nullable = false)
    private ExportFormat exportFormat = ExportFormat.PDF;

    @PrePersist
    protected void onCreate() { generatedAt = LocalDateTime.now(); }

    public enum ReportType {
        TranscriptOfRecords, GradeCard, SummaryOfGrades, GradeCertificate,
        EnrollmentStatistics, CHEDReport, Other
    }
    public enum ExportFormat { PDF, XLSX }
}

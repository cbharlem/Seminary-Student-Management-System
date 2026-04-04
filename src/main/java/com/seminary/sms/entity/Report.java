package com.seminary.sms.entity;

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

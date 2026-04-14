package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (EntranceExam)
// Maps to the database table: tblentranceexam
// Records the result of an entrance exam taken by an applicant.
// Stores the exam date, score, maximum possible score, result (Passed/Failed/Pending),
// and any remarks from the examiner.
//
// Relationships:
//   @ManyToOne → Applicant   (an applicant can take multiple exams; each exam links to one applicant)
//
// LAYER 5 → LAYER 4: EntranceExamRepository queries tblentranceexam using this entity.
// LAYER 4 → LAYER 5: Queries return EntranceExam objects.
// LAYER 5 → LAYER 3: ApplicantService uses this to record and retrieve exam results.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblentranceexam")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EntranceExam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldExamID", nullable = false, unique = true, length = 30)
    private String examId;

    // An applicant can take multiple exams — fldApplicantIndex is the foreign key
    // FetchType.LAZY defers loading the Applicant object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldApplicantIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Applicant applicant;

    @Column(name = "fldExamDate", nullable = false)         private LocalDate examDate;
    @Column(name = "fldScore", precision = 5, scale = 2)    private BigDecimal score;
    @Column(name = "fldMaxScore", precision = 5, scale = 2) private BigDecimal maxScore;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "fldResult", nullable = false)
    private ExamResult result = ExamResult.Pending;

    @Column(name = "fldRemarks", columnDefinition = "TEXT") private String remarks;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ExamResult { Passed, Failed, Pending }
}

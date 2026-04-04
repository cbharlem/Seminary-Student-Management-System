package com.seminary.sms.entity;

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

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ExamResult { Passed, Failed, Pending }
}

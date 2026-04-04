package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblgrades")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldGradeID", nullable = false, unique = true, length = 30)
    private String gradeId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnrollmentSubjectIndex", nullable = false, unique = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private EnrollmentSubject enrollmentSubject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Column(name = "fldMidtermGrade", precision = 3, scale = 2) private BigDecimal midtermGrade;
    @Column(name = "fldFinalGrade",   precision = 3, scale = 2) private BigDecimal finalGrade;
    @Column(name = "fldFinalRating",  precision = 3, scale = 2) private BigDecimal finalRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldGradeStatus", nullable = false)
    @Builder.Default
    private GradeStatus gradeStatus = GradeStatus.NotYetGraded;

    @Column(name = "fldRemarks", length = 100) private String remarks;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnteredByUserIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User enteredBy;

    @Column(name = "fldDateEntered")                            private LocalDateTime dateEntered;
    @Column(name = "fldLastModifiedAt", nullable = false)       private LocalDateTime lastModifiedAt;

    @PrePersist
    protected void onCreate() { lastModifiedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { lastModifiedAt = LocalDateTime.now(); }

    public void computeFinalRating() {
        if (midtermGrade != null && finalGrade != null) {
            finalRating = midtermGrade.add(finalGrade)
                .divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            gradeStatus = finalRating.compareTo(new BigDecimal("3.0")) <= 0
                ? GradeStatus.Passed : GradeStatus.Failed;
        }
    }

    public enum GradeStatus { Passed, Failed, Incomplete, Dropped, NotYetGraded }
}

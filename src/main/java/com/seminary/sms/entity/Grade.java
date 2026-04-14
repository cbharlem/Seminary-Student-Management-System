package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Grade)
// Maps to the database table: tblgrades
// Stores the grade for one subject taken by a student in one semester.
// Contains midterm grade, final grade, and a computed final rating (average).
// Also tracks who entered the grade and when it was last modified.
//
// Relationships:
//   @OneToOne  → EnrollmentSubject   (one grade record per enrolled subject)
//   @ManyToOne → Student             (the student who received the grade)
//   @ManyToOne → Course              (the subject being graded)
//   @ManyToOne → Semester            (the semester this grade belongs to)
//   @ManyToOne → User                (the registrar who entered the grade)
//
// LAYER 5 → LAYER 4: GradeRepository queries tblgrades using this entity.
// LAYER 4 → LAYER 5: Queries return Grade objects.
// LAYER 5 → LAYER 3: GradeService computes final ratings and saves grades.
// ─────────────────────────────────────────────────────────────────────────────

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

    // One grade record belongs to exactly one enrolled subject — fldEnrollmentSubjectIndex is the foreign key
    // unique = true enforces that only one grade can exist per enrolled subject
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnrollmentSubjectIndex", nullable = false, unique = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private EnrollmentSubject enrollmentSubject;

    // The student who received this grade — fldStudentIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    // The course being graded — fldCourseIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    // The semester this grade belongs to — fldSemesterIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    // Midterm components — entered by registrar
    @Column(name = "fldMidtermClassStanding", precision = 5, scale = 2) private BigDecimal midtermClassStanding;
    @Column(name = "fldMidtermExam",          precision = 5, scale = 2) private BigDecimal midtermExam;
    // Midterm term grade — computed: (CS × 60%) + (Exam × 40%)
    @Column(name = "fldMidtermGrade", precision = 5, scale = 2) private BigDecimal midtermGrade;

    // Final components — entered by registrar
    @Column(name = "fldFinalClassStanding", precision = 5, scale = 2) private BigDecimal finalClassStanding;
    @Column(name = "fldFinalExam",          precision = 5, scale = 2) private BigDecimal finalExam;
    // Final term grade — computed: (CS × 60%) + (Exam × 40%)
    @Column(name = "fldFinalGrade",  precision = 5, scale = 2) private BigDecimal finalGrade;

    // Course final rating — computed: (Midterm Grade + Final Grade) / 2
    @Column(name = "fldFinalRating", precision = 5, scale = 2) private BigDecimal finalRating;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldGradeStatus", nullable = false)
    @Builder.Default
    private GradeStatus gradeStatus = GradeStatus.NotYetGraded;

    @Column(name = "fldRemarks", length = 100) private String remarks;

    // The registrar user who entered or last modified this grade — fldEnteredByUserIndex is the foreign key
    // Nullable — a grade may exist before it is entered by a staff member
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnteredByUserIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User enteredBy;

    @Column(name = "fldDateEntered")                            private LocalDateTime dateEntered;
    @Column(name = "fldLastModifiedAt", nullable = false)       private LocalDateTime lastModifiedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { lastModifiedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { lastModifiedAt = LocalDateTime.now(); }

    /**
     * Option A — CHED grading formula:
     *   Term Grade  = (Class Standing × 0.60) + (Exam × 0.40)
     *   Final Rating = (Midterm Grade + Final Grade) / 2
     * Skips computation if status is manually set to Incomplete or Dropped.
     */
    public void computeFinalRating() {
        if (gradeStatus == GradeStatus.Incomplete || gradeStatus == GradeStatus.Dropped) return;

        // Compute midterm term grade from components
        if (midtermClassStanding != null && midtermExam != null) {
            midtermGrade = midtermClassStanding.multiply(new BigDecimal("0.60"))
                .add(midtermExam.multiply(new BigDecimal("0.40")))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Compute final term grade from components
        if (finalClassStanding != null && finalExam != null) {
            finalGrade = finalClassStanding.multiply(new BigDecimal("0.60"))
                .add(finalExam.multiply(new BigDecimal("0.40")))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Compute course final rating from both term grades
        if (midtermGrade != null && finalGrade != null) {
            finalRating = midtermGrade.add(finalGrade)
                .divide(new BigDecimal("2"), 2, java.math.RoundingMode.HALF_UP);
            gradeStatus = finalRating.compareTo(new BigDecimal("3.0")) <= 0
                ? GradeStatus.Passed : GradeStatus.Failed;
        }
    }

    public enum GradeStatus { Passed, Failed, Incomplete, Dropped, NotYetGraded }
}

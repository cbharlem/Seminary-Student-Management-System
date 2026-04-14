package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (EnrollmentSubject)
// Maps to the database table: tblenrollmentsubjects
// Represents a single subject (course) that a student has enrolled in
// as part of their semester enrollment. Think of it as one line item
// on a student's class card — one row per subject per enrollment.
//
// Relationships:
//   @ManyToOne → Enrollment   (links back to the overall semester enrollment)
//   @ManyToOne → Course       (the specific subject being taken)
//   @ManyToOne → Schedule     (the class schedule assigned for this subject)
//
// LAYER 5 → LAYER 4: EnrollmentSubjectRepository queries this table.
// LAYER 4 → LAYER 5: Queries return EnrollmentSubject objects.
// LAYER 5 → LAYER 3: EnrollmentService and GradeService work with these objects.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblenrollmentsubjects")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EnrollmentSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldEnrollmentSubjectID", nullable = false, unique = true, length = 30)
    private String enrollmentSubjectId;

    // Links this subject line item back to the student's overall enrollment record
    // fldEnrollmentIndex is the foreign key; FetchType.LAZY defers loading Enrollment until needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnrollmentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Enrollment enrollment;

    // The specific course the student is taking — fldCourseIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    // The class schedule assigned for this subject (nullable — may not be assigned yet)
    // fldScheduleIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldScheduleIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldStatus", nullable = false)
    @Builder.Default
    private SubjectStatus status = SubjectStatus.Enrolled;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets the creation timestamp — cannot be changed later (updatable = false on the column)
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum SubjectStatus { Enrolled, Dropped, Completed, Failed, Incomplete }
}

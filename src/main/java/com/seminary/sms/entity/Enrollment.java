package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Enrollment)
// Maps to the database table: tblenrollment
// Represents one enrollment record for a student in a specific semester.
// Each time a student officially enrolls for a semester, one row is created here.
// Tracks the program, year level, enrollment date, and status (Enrolled, Dropped, etc.).
//
// Relationships:
//   @ManyToOne → Student    (one student can have many enrollment records over time)
//   @ManyToOne → Program    (the program the student is enrolled in)
//   @ManyToOne → Semester   (the semester this enrollment belongs to)
//
// LAYER 5 → LAYER 4: EnrollmentRepository queries tblenrollment using this entity.
// LAYER 4 → LAYER 5: Queries return Enrollment objects.
// LAYER 5 → LAYER 3: EnrollmentService uses Enrollment to enroll students in subjects.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblenrollment")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldEnrollmentID", nullable = false, unique = true, length = 30)
    private String enrollmentId;

    // One student can have many enrollment records over time — fldStudentIndex is the foreign key
    // FetchType.LAZY defers loading the Student object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    // Many enrollments can belong to the same program — fldProgramIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    // Many enrollments can belong to the same semester — fldSemesterIndex is the foreign key
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Column(name = "fldYearLevel", nullable = false, columnDefinition = "TINYINT")     private Integer yearLevel;
    @Column(name = "fldEnrollmentDate", nullable = false) private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldEnrollmentStatus", nullable = false)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.Enrolled;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum EnrollmentStatus { Enrolled, Dropped, LOA, Withdrawn }
}

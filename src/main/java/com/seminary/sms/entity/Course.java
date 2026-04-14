package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Course)
// Maps to the database table: tblcourses
// Represents a subject or course that belongs to a program (e.g., "Philosophy 101").
// Stores the course code, name, number of units, year level, and semester it belongs to.
//
// Relationships:
//   @ManyToOne → Program   (many courses belong to one academic program)
//
// LAYER 5 → LAYER 4: CourseRepository queries tblcourses using this entity.
// LAYER 4 → LAYER 5: Queries return Course objects.
// LAYER 5 → LAYER 2: CurriculumController and EnrollmentController use Course objects.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblcourses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldCourseID", nullable = false, unique = true, length = 30)
    private String courseId;

    @Column(name = "fldCourseCode", nullable = false, unique = true, length = 30)
    private String courseCode;

    @Column(name = "fldCourseName", nullable = false, length = 100)
    private String courseName;

    @Column(name = "fldUnits", nullable = false, columnDefinition = "TINYINT")
    private Integer units;

    // Many courses can belong to the same program — fldProgramIndex is the foreign key column
    // FetchType.LAZY defers loading the Program object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldYearLevel", nullable = false, columnDefinition = "TINYINT")
    private Integer yearLevel;

    @Column(name = "fldSemesterNumber", nullable = false, columnDefinition = "TINYINT")
    private Integer semesterNumber;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fldUpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

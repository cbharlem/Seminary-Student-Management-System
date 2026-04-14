package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Prerequisite)
// Maps to the database table: tblprerequisites
// Defines a prerequisite rule between two courses: a student must pass
// the prerequisite course before they are allowed to enroll in the main course.
// Each row is one rule: "to take Course A, you must have passed Course B."
//
// Relationships:
//   @ManyToOne → Course (as "course")             — the course that has the requirement
//   @ManyToOne → Course (as "prerequisiteCourse") — the course that must be passed first
//
// LAYER 5 → LAYER 4: PrerequisiteRepository queries tblprerequisites using this entity.
// LAYER 4 → LAYER 5: Queries return Prerequisite objects.
// LAYER 5 → LAYER 3: EnrollmentService checks prerequisites before enrolling a subject.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblprerequisites")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Prerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldPrerequisiteID", nullable = false, unique = true, length = 30)
    private String prerequisiteId;

    // The course that has the requirement — fldCourseIndex is the foreign key
    // Example: "Philosophy 102" is the course a student wants to take
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    // The course that must be passed first — fldPrerequisiteIndex is the foreign key
    // Example: "Philosophy 101" is what must have been passed before the student can take the course above
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldPrerequisiteIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course prerequisiteCourse;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets the creation timestamp — prerequisite rules are never updated, only added or deleted
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (StudentSection)
// Maps to the database table: tblstudentsection
// This is a join/assignment table that records which section a student belongs to
// in a given semester. It links a Student to a Section for one Semester.
// When a student is enrolled, they are also assigned to a section through this table.
//
// Relationships:
//   @ManyToOne → Student    (the student being assigned)
//   @ManyToOne → Section    (the section they are placed in)
//   @ManyToOne → Semester   (the semester this assignment is for)
//
// LAYER 5 → LAYER 4: StudentSectionRepository queries tblstudentsection using this entity.
// LAYER 4 → LAYER 5: Queries return StudentSection objects.
// LAYER 5 → LAYER 3: EnrollmentService creates a StudentSection record during enrollment.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblstudentsection")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldStudentSectionID", nullable = false, unique = true, length = 30)
    private String studentSectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSectionIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Column(name = "fldDateAssigned", nullable = false) private LocalDate dateAssigned;
    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

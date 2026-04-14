package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Semester)
// Maps to the database table: tblsemester
// Represents one semester within a school year (e.g., "First Semester 2024-2025").
// Stores the semester number, label, start and end dates, and active flag.
// Only one semester should be marked active at a time — the system uses
// the active semester as the default context for enrollment and grades.
//
// Relationships:
//   @ManyToOne → SchoolYear   (each semester belongs to one school year)
//
// LAYER 5 → LAYER 4: SemesterRepository queries tblsemester using this entity.
// LAYER 4 → LAYER 5: Queries return Semester objects (including the active semester lookup).
// LAYER 5 → LAYER 2: Multiple controllers use the active semester to scope their data.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblsemester")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSemesterID", nullable = false, unique = true, length = 20)
    private String semesterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSchoolYearIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private SchoolYear schoolYear;

    @Column(name = "fldSemesterNumber", nullable = false, columnDefinition = "TINYINT")
    private Integer semesterNumber;

    @Column(name = "fldSemesterLabel", nullable = false, length = 50)
    private String semesterLabel;

    @Column(name = "fldStartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "fldEndDate", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = false;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

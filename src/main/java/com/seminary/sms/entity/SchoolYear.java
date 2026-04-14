package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (SchoolYear)
// Maps to the database table: tblschoolyear
// Represents one academic school year (e.g., "2024-2025").
// Only one school year should be marked as active at a time.
// Semesters belong to a school year via a @ManyToOne on the Semester side.
//
// Relationships:
//   (none — SchoolYear is referenced by Semester and Application via @ManyToOne on their side)
//
// LAYER 5 → LAYER 4: SchoolYearRepository queries tblschoolyear using this entity.
// LAYER 4 → LAYER 5: Queries return SchoolYear objects.
// LAYER 5 → LAYER 2: SchoolYearController manages school year and semester records.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblschoolyear")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSchoolYearID", nullable = false, unique = true, length = 20)
    private String schoolYearId;

    @Column(name = "fldYearLabel", nullable = false, length = 20)
    private String yearLabel;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = false;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

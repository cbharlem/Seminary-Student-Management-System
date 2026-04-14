package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Program)
// Maps to the database table: tblprogram
// Represents an academic program offered by the seminary (e.g., "Bachelor of Philosophy").
// Stores the program code, full name, total units required, duration, and active status.
//
// Relationships:
//   (none — Program is referenced by many other entities via @ManyToOne on their side,
//    such as Course, Section, Enrollment, Alumni, and Applicant)
//
// LAYER 5 → LAYER 4: ProgramRepository queries tblprogram using this entity.
// LAYER 4 → LAYER 5: Queries return Program objects.
// LAYER 5 → LAYER 2: Multiple controllers use Program to filter courses, sections, and students.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblprogram")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Program {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldProgramID", nullable = false, unique = true, length = 30)
    private String programId;

    @Column(name = "fldProgramCode", nullable = false, unique = true, length = 30)
    private String programCode;

    @Column(name = "fldProgramName", nullable = false, length = 100)
    private String programName;

    @Column(name = "fldTotalUnits", nullable = false, columnDefinition = "TINYINT")
    private Integer totalUnits;

    @Column(name = "fldDuration", nullable = false, length = 20)
    private String duration;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fldUpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

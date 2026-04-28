package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Curriculum)
// Maps to the database table: tblcurriculum
// Represents a versioned curriculum for a specific program and school year
// (e.g., "BA Philosophy — 2024-2025 Curriculum").
//
// Relationships:
//   @ManyToOne → Program  (many curriculum versions belong to one program)
//   ← Course             (courses reference which curriculum version they belong to)
//
// Only one Curriculum per Program should have isActive = true at a time.
// Historical (non-active) curricula are preserved as read-only records.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblcurriculum")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Curriculum {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldCurriculumID", nullable = false, unique = true, length = 30)
    private String curriculumId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldLabel", nullable = false, length = 60)
    private String label;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = false;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

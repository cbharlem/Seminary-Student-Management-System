package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Alumni)
// Maps to the database table: tblalumni
// This class represents a student who has already graduated.
// Spring/Hibernate reads and writes rows in tblalumni using this class.
//
// Relationships:
//   @OneToOne  → Student   (one alumni record belongs to exactly one student)
//   @ManyToOne → Program   (many alumni can belong to the same program)
//
// LAYER 5 → LAYER 4: This entity is used by AlumniRepository to run DB queries.
// LAYER 4 → LAYER 5: The repository returns Alumni objects back to the service.
// LAYER 5 → LAYER 3: AlumniService receives Alumni objects and applies business logic.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblalumni")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Alumni {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldAlumniID", nullable = false, unique = true, length = 30)
    private String alumniId;

    // Links this alumni record to one student — fldStudentIndex is the foreign key column
    // FetchType.LAZY means the Student is only loaded from the DB when actually accessed (saves memory)
    // @ToString.Exclude / @EqualsAndHashCode.Exclude prevent infinite loops when Lombok generates those methods
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false, unique = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @Column(name = "fldGraduationDate", nullable = false) private LocalDate graduationDate;

    // Many alumni can belong to the same program — fldProgramIndex is the foreign key column
    // FetchType.LAZY defers loading the Program object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldYearGraduated", nullable = false, length = 20) private String yearGraduated;
    @Column(name = "fldHonors", length = 100)           private String honors;
    @Column(name = "fldCurrentMinistry", length = 255)  private String currentMinistry;
    @Column(name = "fldCurrentAddress", length = 255)   private String currentAddress;
    @Column(name = "fldNotes", columnDefinition = "TEXT") private String notes;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets both timestamps to right now so the record always has a creation time
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    // Keeps updatedAt current so we always know when a record was last changed
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

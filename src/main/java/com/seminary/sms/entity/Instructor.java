package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Instructor)
// Maps to the database table: tblinstructors
// Represents a teacher or faculty member who can be assigned to class schedules.
// Stores their name, contact info, specialization, and whether they are active.
//
// Relationships:
//   (none — Instructor is referenced by Schedule via @ManyToOne on the Schedule side)
//
// LAYER 5 → LAYER 4: InstructorRepository queries tblinstructors using this entity.
// LAYER 4 → LAYER 5: Queries return Instructor objects.
// LAYER 5 → LAYER 2: SectionController uses Instructor objects when managing class schedules.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblinstructors")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Instructor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldInstructorID", nullable = false, unique = true, length = 30)
    private String instructorId;

    @Column(name = "fldFirstName", nullable = false, length = 50) private String firstName;
    @Column(name = "fldMiddleName", length = 50)                  private String middleName;
    @Column(name = "fldLastName", nullable = false, length = 50)  private String lastName;
    @Column(name = "fldEmail", length = 100)                      private String email;
    @Column(name = "fldContactNumber", length = 20)               private String contactNumber;
    @Column(name = "fldSpecialization", length = 100)             private String specialization;
    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT") private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    // Convenience method — assembles the instructor's full name including optional middle name
    // Called by: schedule display logic and any place that needs a formatted name string
    public String getFullName() {
        return firstName + " " + (middleName != null ? middleName + " " : "") + lastName;
    }
}

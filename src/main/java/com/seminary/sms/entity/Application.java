package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Application)
// Maps to the database table: tblapplications
// Represents a formal application submitted by an Applicant for a given school year.
// Tracks the application's progress through stages like Applied, Interviewed,
// Confirmed, Admitted, Enrolled, or Rejected.
//
// Relationships:
//   @ManyToOne → Applicant   (one applicant can submit multiple applications)
//   @ManyToOne → SchoolYear  (each application belongs to a school year)
//
// LAYER 5 → LAYER 4: ApplicationRepository queries tblapplications using this entity.
// LAYER 4 → LAYER 5: Queries return Application objects.
// LAYER 5 → LAYER 3: ApplicantService uses Application to manage status updates.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblapplications")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldApplicationID", nullable = false, unique = true, length = 30)
    private String applicationId;

    // Many applications can belong to the same applicant — fldApplicantIndex is the foreign key
    // FetchType.LAZY defers loading the Applicant object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldApplicantIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Applicant applicant;

    @Column(name = "fldApplicationDate", nullable = false)
    private LocalDate applicationDate;

    // Many applications can belong to the same school year — fldSchoolYearIndex is the foreign key
    // FetchType.LAZY defers loading the SchoolYear object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSchoolYearIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private SchoolYear schoolYear;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "fldApplicationStatus", nullable = false)
    private ApplicationStatus applicationStatus = ApplicationStatus.Applied;

    @Column(name = "fldInterviewDate")   private LocalDate interviewDate;
    @Column(name = "fldConventionDate")  private LocalDate conventionDate;
    @Column(name = "fldRejectionReason", length = 255) private String rejectionReason;
    @Column(name = "fldRemarks", columnDefinition = "TEXT") private String remarks;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ApplicationStatus {
        Applied, Interviewed, AspiringConventionAttended, Confirmed, Admitted, Enrolled, Rejected, Withdrawn
    }
}

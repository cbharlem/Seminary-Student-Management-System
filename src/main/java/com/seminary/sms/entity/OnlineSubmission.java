package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (OnlineSubmission)
// Maps to the database table: tblonline_submissions
// Represents a student-submitted online admission application that has NOT yet
// been reviewed or accepted by the registrar.
//
// These records are UNVALIDATED — they come directly from the public /apply.html
// form without requiring a login. The registrar reviews each submission and either:
//   - Accepts it  → creates an official Applicant + Application record
//   - Rejects it  → records a reason, no further action
//
// This separation ensures that tblapplicants only contains registrar-approved data.
//
// Relationships:
//   @ManyToOne → Program   (the program the student applied for)
//
// LAYER 5 → LAYER 4: OnlineSubmissionRepository uses this entity to query tblonline_submissions.
// LAYER 4 → LAYER 5: Queries return OnlineSubmission objects.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblonline_submissions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OnlineSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSubmissionID", nullable = false, unique = true, length = 30)
    private String submissionId;

    // ── Personal Information ───────────────────────────────────────────────────
    @Column(name = "fldFirstName", nullable = false, length = 50)  private String firstName;
    @Column(name = "fldMiddleName", length = 50)                   private String middleName;
    @Column(name = "fldLastName", nullable = false, length = 50)   private String lastName;
    @Column(name = "fldDateOfBirth", nullable = false)              private LocalDate dateOfBirth;
    @Column(name = "fldPlaceOfBirth", length = 100)                private String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldGender")
    private Applicant.Gender gender;

    // ── Contact & Identity ────────────────────────────────────────────────────
    @Column(name = "fldAddress", length = 255)                     private String address;
    @Column(name = "fldContactNumber", length = 20)                private String contactNumber;
    @Column(name = "fldEmail", nullable = false, length = 100)     private String email;
    @Column(name = "fldNationality", length = 50)                  private String nationality;
    @Column(name = "fldReligion", length = 50)                     private String religion;

    // ── Family Background ─────────────────────────────────────────────────────
    @Column(name = "fldFatherName", length = 100)                  private String fatherName;
    @Column(name = "fldFatherOccupation", length = 100)            private String fatherOccupation;
    @Column(name = "fldMotherName", length = 100)                  private String motherName;
    @Column(name = "fldMotherOccupation", length = 100)            private String motherOccupation;
    @Column(name = "fldGuardianName", length = 100)                private String guardianName;
    @Column(name = "fldGuardianContact", length = 20)              private String guardianContact;

    // ── Previous Education ────────────────────────────────────────────────────
    @Column(name = "fldLastSchoolAttended", length = 150)          private String lastSchoolAttended;
    @Column(name = "fldLastSchoolYear", length = 20)               private String lastSchoolYear;
    @Column(name = "fldLastYearLevel", length = 50)                private String lastYearLevel;

    // ── Seminary Enrollment ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "fldSeminaryLevel")
    private Applicant.SeminaryLevel seminaryLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program appliedProgram;

    // ── Submission Tracking ───────────────────────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "fldStatus", nullable = false)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.Pending;

    @Column(name = "fldRejectionReason", length = 255)
    private String rejectionReason;

    @Column(name = "fldSubmittedAt", nullable = false, updatable = false)
    private LocalDateTime submittedAt;

    @Column(name = "fldReviewedAt")
    private LocalDateTime reviewedAt;

    // Called automatically by Hibernate just before a new row is INSERTed
    @PrePersist
    protected void onCreate() { submittedAt = LocalDateTime.now(); }

    public enum SubmissionStatus { Pending, Accepted, Rejected }
}

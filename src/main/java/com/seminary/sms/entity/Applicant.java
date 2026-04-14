package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Applicant)
// Maps to the database table: tblapplicants
// Represents a person who has applied to the seminary but is not yet a student.
// Stores personal info, family background, and school history of the applicant.
//
// Relationships:
//   @ManyToOne → Program   (the program the applicant applied for)
//
// Also contains a @Transient field "applicationStatus" — this field is NOT saved
// to the database. It is filled in at runtime by a query.
//
// LAYER 5 → LAYER 4: ApplicantRepository uses this entity to query tblapplicants.
// LAYER 4 → LAYER 5: Queries return Applicant objects.
// LAYER 5 → LAYER 3: ApplicantService receives and processes Applicant objects.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblapplicants")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldApplicantID", nullable = false, unique = true, length = 30)
    private String applicantId;

    @Column(name = "fldFirstName", nullable = false, length = 50) private String firstName;
    @Column(name = "fldMiddleName", length = 50)                  private String middleName;
    @Column(name = "fldLastName", nullable = false, length = 50)  private String lastName;
    @Column(name = "fldDateOfBirth", nullable = false)             private LocalDate dateOfBirth;
    @Column(name = "fldPlaceOfBirth", length = 100)               private String placeOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldGender")
    private Gender gender;

    @Column(name = "fldAddress", length = 255)        private String address;
    @Column(name = "fldContactNumber", length = 20)   private String contactNumber;
    @Column(name = "fldEmail", nullable = false, length = 100) private String email;
    @Column(name = "fldNationality", length = 50)     private String nationality;
    @Column(name = "fldReligion", length = 50)        private String religion;
    @Column(name = "fldFatherName", length = 100)     private String fatherName;
    @Column(name = "fldFatherOccupation", length = 100) private String fatherOccupation;
    @Column(name = "fldMotherName", length = 100)        private String motherName;
    @Column(name = "fldMotherOccupation", length = 100)  private String motherOccupation;
    @Column(name = "fldGuardianName", length = 100)      private String guardianName;
    @Column(name = "fldGuardianContact", length = 20)    private String guardianContact;
    @Column(name = "fldLastSchoolAttended", length = 150) private String lastSchoolAttended;
    @Column(name = "fldLastSchoolYear", length = 20)  private String lastSchoolYear;
    @Column(name = "fldLastYearLevel", length = 50)   private String lastYearLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldSeminaryLevel")
    private SeminaryLevel seminaryLevel;

    // Many applicants can apply to the same program — fldProgramIndex is the foreign key column
    // FetchType.LAZY defers loading the Program object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program appliedProgram;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    // @Transient — NOT saved to the database
    // Populated at runtime by joining with the Application table so callers can see the applicant's status
    @Transient
    private String applicationStatus;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets both timestamps to right now so the record always has a creation time
    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    // Called automatically by Hibernate just before an existing row is UPDATEd
    // Keeps updatedAt current so we always know when the applicant's record was last modified
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Gender { Male, Female, Other }
    public enum SeminaryLevel { Propaedeutic, College }
}

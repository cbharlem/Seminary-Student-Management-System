package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Student)
// This class IS the database table in Java form. Every field here maps to
//   a column in the actual database table.
// LAYER 4 → LAYER 5: Repository uses this class to know what table and columns
//   exist so it can build the correct SQL queries automatically.
// LAYER 5 → LAYER 4: When the database returns a row, Spring maps each column
//   back to the matching field in this class to build a Student object.
// LAYER 5 → LAYER 1: This object travels up through Repository → Service →
//   Controller, where Spring converts it to JSON and sends it to the browser.
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "tblstudents") // maps this class to the tblstudents database table
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex") // primary key — the row number in the table
    private Integer index;

    @Column(name = "fldStudentID", nullable = false, unique = true, length = 30)
    private String studentId; // maps to fldStudentID column

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldApplicationIndex", nullable = false, unique = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldUserIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private User user;

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
    @Column(name = "fldMotherName", length = 100)     private String motherName;
    @Column(name = "fldMotherOccupation", length = 100) private String motherOccupation;
    @Column(name = "fldGuardianName", length = 100)   private String guardianName;
    @Column(name = "fldGuardianContact", length = 20) private String guardianContact;
    @Column(name = "fldBloodType", length = 5)        private String bloodType;
    @Column(name = "fldMedicalConditions", columnDefinition = "TEXT") private String medicalConditions;
    @Column(name = "fldAllergies", columnDefinition = "TEXT")         private String allergies;
    @Column(name = "fldBaptismDate")                  private LocalDate baptismDate;
    @Column(name = "fldBaptismChurch", length = 150)  private String baptismChurch;
    @Column(name = "fldConfirmationDate")             private LocalDate confirmationDate;
    @Column(name = "fldConfirmationChurch", length = 150) private String confirmationChurch;
    @Column(name = "fldParishPriest", length = 100)   private String parishPriest;
    @Column(name = "fldDiocese", length = 100)        private String diocese;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldSeminaryLevel", nullable = false)
    private SeminaryLevel seminaryLevel;

    @Builder.Default
    @Column(name = "fldCurrentYearLevel", nullable = false, columnDefinition = "TINYINT")
    private Integer currentYearLevel = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "fldCurrentStatus", nullable = false)
    private StudentStatus currentStatus = StudentStatus.Active;

    // LAYER 5 → LAYER 5: This field links Student to the Program entity.
    // @ManyToOne — many students belong to one program
    // @JoinColumn — the foreign key column in tblstudents that stores the program's index
    // When Repository fetches a Student, Spring automatically follows this link,
    //   queries tblprogram, and puts the full Program object here instead of just a number.
    // LAYER 5 → LAYER 1: The Program object inside here becomes program: {...} in the JSON
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getFullName() {
        return firstName + " " + (middleName != null ? middleName + " " : "") + lastName;
    }

    public enum Gender { Male, Female, Other }
    public enum SeminaryLevel { Propaedeutic, College }
    public enum StudentStatus { Active, Inactive, LOA, Dismissed, Graduated, Alumni }
}

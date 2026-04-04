package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblstudents")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldStudentID", nullable = false, unique = true, length = 30)
    private String studentId;

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

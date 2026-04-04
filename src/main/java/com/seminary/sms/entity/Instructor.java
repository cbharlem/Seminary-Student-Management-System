package com.seminary.sms.entity;

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

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public String getFullName() {
        return firstName + " " + (middleName != null ? middleName + " " : "") + lastName;
    }
}

package com.seminary.sms.entity;

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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false, unique = true)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @Column(name = "fldGraduationDate", nullable = false) private LocalDate graduationDate;

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

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

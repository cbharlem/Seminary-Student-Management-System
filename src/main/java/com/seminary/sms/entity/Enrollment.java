package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblenrollment")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldEnrollmentID", nullable = false, unique = true, length = 30)
    private String enrollmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Column(name = "fldYearLevel", nullable = false, columnDefinition = "TINYINT")     private Integer yearLevel;
    @Column(name = "fldEnrollmentDate", nullable = false) private LocalDate enrollmentDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldEnrollmentStatus", nullable = false)
    @Builder.Default
    private EnrollmentStatus enrollmentStatus = EnrollmentStatus.Enrolled;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum EnrollmentStatus { Enrolled, Dropped, LOA, Withdrawn }
}

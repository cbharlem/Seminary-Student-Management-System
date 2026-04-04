package com.seminary.sms.entity;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldApplicantIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Applicant applicant;

    @Column(name = "fldApplicationDate", nullable = false)
    private LocalDate applicationDate;

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

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ApplicationStatus {
        Applied, Interviewed, AspiringConventionAttended, Confirmed, Admitted, Enrolled, Rejected, Withdrawn
    }
}

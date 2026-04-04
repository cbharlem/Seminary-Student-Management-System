package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblenrollmentsubjects")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class EnrollmentSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldEnrollmentSubjectID", nullable = false, unique = true, length = 30)
    private String enrollmentSubjectId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldEnrollmentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldScheduleIndex")
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldStatus", nullable = false)
    @Builder.Default
    private SubjectStatus status = SubjectStatus.Enrolled;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum SubjectStatus { Enrolled, Dropped, Completed, Failed, Incomplete }
}

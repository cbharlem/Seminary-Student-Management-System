package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblcourses")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldCourseID", nullable = false, unique = true, length = 30)
    private String courseId;

    @Column(name = "fldCourseCode", nullable = false, unique = true, length = 30)
    private String courseCode;

    @Column(name = "fldCourseName", nullable = false, length = 100)
    private String courseName;

    @Column(name = "fldUnits", nullable = false, columnDefinition = "TINYINT")
    private Integer units;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldYearLevel", nullable = false, columnDefinition = "TINYINT")
    private Integer yearLevel;

    @Column(name = "fldSemesterNumber", nullable = false, columnDefinition = "TINYINT")
    private Integer semesterNumber;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fldUpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

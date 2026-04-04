package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblsemester")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSemesterID", nullable = false, unique = true, length = 20)
    private String semesterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSchoolYearIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private SchoolYear schoolYear;

    @Column(name = "fldSemesterNumber", nullable = false, columnDefinition = "TINYINT")
    private Integer semesterNumber;

    @Column(name = "fldSemesterLabel", nullable = false, length = 50)
    private String semesterLabel;

    @Column(name = "fldStartDate", nullable = false)
    private LocalDate startDate;

    @Column(name = "fldEndDate", nullable = false)
    private LocalDate endDate;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = false;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

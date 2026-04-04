package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblprerequisites")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Prerequisite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldPrerequisiteID", nullable = false, unique = true, length = 30)
    private String prerequisiteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldPrerequisiteIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course prerequisiteCourse;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

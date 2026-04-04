package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblstudentsection")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class StudentSection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldStudentSectionID", nullable = false, unique = true, length = 30)
    private String studentSectionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSectionIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Column(name = "fldDateAssigned", nullable = false) private LocalDate dateAssigned;
    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

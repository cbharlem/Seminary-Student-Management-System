package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblsection")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSectionID", nullable = false, unique = true, length = 30)
    private String sectionId;

    @Column(name = "fldSectionCode", nullable = false, length = 30) private String sectionCode;
    @Column(name = "fldSectionName", nullable = false, length = 50) private String sectionName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldProgramIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Program program;

    @Column(name = "fldYearLevel", nullable = false, columnDefinition = "TINYINT") private Integer yearLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSemesterIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Semester semester;

    @Builder.Default
    @Column(name = "fldCapacity", columnDefinition = "TINYINT") private Integer capacity = 40;
    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT") private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}

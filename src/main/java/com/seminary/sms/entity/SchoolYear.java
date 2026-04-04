package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblschoolyear")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SchoolYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldSchoolYearID", nullable = false, unique = true, length = 20)
    private String schoolYearId;

    @Column(name = "fldYearLabel", nullable = false, length = 20)
    private String yearLabel;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = false;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}

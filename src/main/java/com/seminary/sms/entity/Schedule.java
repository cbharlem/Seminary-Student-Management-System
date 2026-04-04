package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "tblschedule")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldScheduleID", nullable = false, unique = true, length = 30)
    private String scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldSectionIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldCourseIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldInstructorIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Instructor instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldRoomIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldDayOfWeek", nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(name = "fldTimeStart", nullable = false) private LocalTime timeStart;
    @Column(name = "fldTimeEnd",   nullable = false) private LocalTime timeEnd;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "fldUpdatedAt", nullable = false)                    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum DayOfWeek { Monday, Tuesday, Wednesday, Thursday, Friday, Saturday, Sunday }
}

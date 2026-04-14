package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Room)
// Maps to the database table: tblrooms
// Represents a physical classroom or venue that can be assigned to a class schedule.
// Stores the room name, building, seating capacity, and whether it is still active.
//
// Relationships:
//   (none — Room is referenced by Schedule via @ManyToOne on the Schedule side)
//
// LAYER 5 → LAYER 4: RoomRepository queries tblrooms using this entity.
// LAYER 4 → LAYER 5: Queries return Room objects.
// LAYER 5 → LAYER 2: SectionController and ScheduleController use Room when creating schedules.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblrooms")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldRoomID", nullable = false, unique = true, length = 30)
    private String roomId;

    @Column(name = "fldRoomName", nullable = false, length = 50) private String roomName;
    @Column(name = "fldBuilding", length = 50)                  private String building;
    @Column(name = "fldCapacity", columnDefinition = "TINYINT") private Integer capacity;
    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT") private Boolean isActive = true;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false) private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }
}



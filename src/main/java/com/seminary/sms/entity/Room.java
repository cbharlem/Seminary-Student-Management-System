package com.seminary.sms.entity;

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

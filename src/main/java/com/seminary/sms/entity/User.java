package com.seminary.sms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tblusers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldUserID", nullable = false, unique = true, length = 30)
    private String userId;

    @Column(name = "fldUsername", nullable = false, unique = true, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "fldPasswordHash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldRole", nullable = false)
    private Role role;

    @Builder.Default
    @Column(name = "fldIsActive", nullable = false, columnDefinition = "TINYINT")
    private Boolean isActive = true;

    @Lob
    @JsonIgnore
    @Column(name = "fldProfilePicture", columnDefinition = "LONGBLOB")
    private byte[] profilePicture;

    @JsonIgnore
    @Column(name = "fldProfilePictureType", length = 50)
    private String profilePictureType;

    @Column(name = "fldCreatedAt", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fldUpdatedAt", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }
    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Role { Registrar, Student }
}

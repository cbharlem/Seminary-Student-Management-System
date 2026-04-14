package com.seminary.sms.entity;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 5 — ENTITY (Document)
// Maps to the database table: tbldocuments
// Represents a file uploaded for a student (e.g., Birth Certificate, Form 137,
// Baptismal Record). Stores the file's name, path on disk, type, and upload time.
//
// Relationships:
//   @ManyToOne → Student   (many documents can belong to one student)
//
// LAYER 5 → LAYER 4: DocumentRepository queries tbldocuments using this entity.
// LAYER 4 → LAYER 5: Queries return Document objects.
// LAYER 5 → LAYER 2: DocumentController reads these objects to serve document lists.
// ─────────────────────────────────────────────────────────────────────────────

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "tbldocuments")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fldIndex")
    private Integer index;

    @Column(name = "fldDocumentID", nullable = false, unique = true, length = 30)
    private String documentId;

    // Many documents can belong to the same student — fldStudentIndex is the foreign key column
    // FetchType.LAZY defers loading the Student object until it is actually needed
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fldStudentIndex", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(name = "fldDocumentType", nullable = false)
    private DocumentType documentType;

    @Column(name = "fldFileName", nullable = false, length = 255) private String fileName;
    @Column(name = "fldFilePath", nullable = false, length = 500) private String filePath;
    @Column(name = "fldUploadedAt", nullable = false, updatable = false) private LocalDateTime uploadedAt;
    @Column(name = "fldRemarks", length = 255) private String remarks;

    // Called automatically by Hibernate just before a new row is INSERTed
    // Sets the upload timestamp — cannot be changed later (updatable = false on the column)
    @PrePersist
    protected void onCreate() { uploadedAt = LocalDateTime.now(); }

    public enum DocumentType {
        BirthCertificate, Form137, Diploma, BaptismalRecord, ConfirmationRecord,
        MarriageContractOfParents, MedicalRecord, DentalRecord,
        ParishPriestRecommendation, Other
    }
}

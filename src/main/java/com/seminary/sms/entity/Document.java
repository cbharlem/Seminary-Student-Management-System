package com.seminary.sms.entity;

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

    @PrePersist
    protected void onCreate() { uploadedAt = LocalDateTime.now(); }

    public enum DocumentType {
        BirthCertificate, Form137, Diploma, BaptismalRecord, ConfirmationRecord,
        MarriageContractOfParents, MedicalRecord, DentalRecord,
        ParishPriestRecommendation, Other
    }
}

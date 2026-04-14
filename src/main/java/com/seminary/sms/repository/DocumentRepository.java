package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (DocumentRepository)
// Serves the Document entity — reads from and writes to the tbldocuments table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByStudent_Index     → returns all documents belonging to a student (by PK)
//   findByStudent_StudentId → returns all documents belonging to a student (by business ID)
//
// LAYER 4 → LAYER 5: Uses the Document entity to map database rows to objects.
// LAYER 4 → LAYER 2: DocumentController calls this repository to serve document lists.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DocumentRepository extends JpaRepository<Document, Integer> {

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: lookups that use the student's integer PK
    List<Document> findByStudent_Index(Integer studentIndex);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldStudentID = ?
    // Called by: DocumentController.getByStudent() to list a student's uploaded documents
    List<Document> findByStudent_StudentId(String studentId);
}

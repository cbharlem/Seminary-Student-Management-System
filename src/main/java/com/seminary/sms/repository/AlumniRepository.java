package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (AlumniRepository)
// Serves the Alumni entity — reads from and writes to the tblalumni table.
//
// This interface extends JpaRepository, which means Spring automatically provides
// standard methods like save(), findAll(), findById(), and deleteById()
// without you writing any SQL.
//
// Custom methods declared here use Spring's method-name convention:
//   findByStudent_Index       → SELECT * FROM tblalumni WHERE fldStudentIndex = ?
//   findByStudent_StudentId   → joins to tblstudents and filters by student ID
//   findByAlumniId            → finds one alumni record by its business ID
//   existsByAlumniId          → returns true/false — useful for duplicate checks
//
// LAYER 4 → LAYER 5: Uses the Alumni entity class to map database rows to objects.
// LAYER 4 → LAYER 3: AlumniService calls this repository to fetch and save alumni data.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Alumni;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AlumniRepository extends JpaRepository<Alumni, Integer> {

    // Auto-generates: SELECT * FROM tblalumni WHERE fldStudentIndex = ?
    // Called by: AlumniService when looking up an alumni record by the student's integer PK
    Optional<Alumni> findByStudent_Index(Integer studentIndex);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldStudentID = ?
    // Called by: AlumniService.getByStudentId() to check if a student is already an alumnus
    Optional<Alumni> findByStudent_StudentId(String studentId);

    // Auto-generates: SELECT * FROM tblalumni WHERE fldAlumniID = ?
    // Called by: AlumniService.unmarkAlumni() and AlumniController.update()
    Optional<Alumni> findByAlumniId(String alumniId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblalumni WHERE fldAlumniID = ?
    // Called by: AlumniController.update() to verify the record exists before saving
    boolean existsByAlumniId(String alumniId);
}

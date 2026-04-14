package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (SectionRepository)
// Serves the Section entity — reads from and writes to the tblsection table.
//
// Spring auto-generates SQL from the method names declared here:
//   findBySectionId           → finds one section by its business ID
//   findBySemester_Index/Id   → all sections for a given semester
//   findByProgram_Index       → all sections belonging to a given program
//
// LAYER 4 → LAYER 5: Uses the Section entity to map database rows to objects.
// LAYER 4 → LAYER 2: SectionController and EnrollmentController call this directly.
// LAYER 4 → LAYER 3: EnrollmentService uses this to assign students to a section.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SectionRepository extends JpaRepository<Section, Integer> {

    // Auto-generates: SELECT * FROM tblsection WHERE fldSectionID = ?
    // Called by: SectionController and EnrollmentService to fetch a specific section record
    Optional<Section> findBySectionId(String sectionId);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldIndex = ?
    // Called by: lookups that retrieve all sections offered in a semester (by PK)
    List<Section> findBySemester_Index(Integer semesterIndex);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldSemesterID = ?
    // Called by: SectionController.getSections() and EnrollmentController to list sections for a given semester
    List<Section> findBySemester_SemesterId(String semesterId);

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldIndex = ?
    // Called by: lookups that filter sections by program (by PK) for enrollment assignment
    List<Section> findByProgram_Index(Integer programIndex);
}

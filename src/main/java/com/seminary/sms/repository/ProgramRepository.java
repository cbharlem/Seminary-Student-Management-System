package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (ProgramRepository)
// Serves the Program entity — reads from and writes to the tblprogram table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByProgramId    → finds one program by its business ID (e.g., "PROG001")
//   findByProgramCode  → finds one program by its short code (e.g., "BPHIL")
//   findByIsActiveTrue → returns only active programs (used to populate dropdowns)
//
// LAYER 4 → LAYER 5: Uses the Program entity to map database rows to objects.
// LAYER 4 → LAYER 2: Multiple controllers call this to look up or list programs.
// LAYER 4 → LAYER 3: EnrollmentService uses this to resolve a program during enrollment.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProgramRepository extends JpaRepository<Program, Integer> {

    // Auto-generates: SELECT * FROM tblprogram WHERE fldProgramID = ?
    // Called by: multiple controllers and EnrollmentService to fetch a specific program by its business ID
    Optional<Program> findByProgramId(String programId);

    // Auto-generates: SELECT * FROM tblprogram WHERE fldProgramCode = ?
    // Called by: lookups that identify a program by its short code (e.g., "BPHIL") rather than its full ID
    Optional<Program> findByProgramCode(String programCode);

    // Auto-generates: SELECT * FROM tblprogram WHERE fldIsActive = 1
    // Called by: CurriculumController and EnrollmentController to populate program dropdowns with active programs only
    List<Program> findByIsActiveTrue();
}

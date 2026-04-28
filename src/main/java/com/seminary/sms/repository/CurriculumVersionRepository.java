package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (CurriculumVersionRepository)
// Serves the Curriculum entity — reads from and writes to the tblcurriculum table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByCurriculumId                              → fetch one curriculum by business ID
//   findByProgram_ProgramIdOrderByCreatedAtDesc     → all versions for a program, newest first
//   findByProgram_ProgramIdAndIsActiveTrue          → the currently active curriculum for a program
//
// LAYER 4 → LAYER 5: Uses the Curriculum entity to map database rows to objects.
// LAYER 4 → LAYER 2: CurriculumController calls this repository.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Curriculum;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CurriculumVersionRepository extends JpaRepository<Curriculum, Integer> {

    // Auto-generates: SELECT * FROM tblcurriculum WHERE fldCurriculumID = ?
    Optional<Curriculum> findByCurriculumId(String curriculumId);

    // Auto-generates: JOIN tblprogram WHERE fldProgramID = ? ORDER BY fldCreatedAt DESC
    // Called by: getCurricula() to list all versions for a program, newest first
    List<Curriculum> findByProgram_ProgramIdOrderByCreatedAtDesc(String programId);

    // Auto-generates: JOIN tblprogram WHERE fldProgramID = ? AND fldIsActive = 1
    // Called by: createCurriculum() to deactivate the old active version before creating a new one
    Optional<Curriculum> findByProgram_ProgramIdAndIsActiveTrue(String programId);
}

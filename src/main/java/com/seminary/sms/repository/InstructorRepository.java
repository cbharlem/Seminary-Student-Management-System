package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (InstructorRepository)
// Serves the Instructor entity — reads from and writes to the tblinstructors table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByInstructorId   → finds one instructor by their business ID
//   existsByInstructorId → returns true/false — used to check before an update
//   findByIsActiveTrue   → returns only active instructors (for dropdown lists)
//
// LAYER 4 → LAYER 5: Uses the Instructor entity to map database rows to objects.
// LAYER 4 → LAYER 2: SectionController calls this directly to manage instructor records.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    // Auto-generates: SELECT * FROM tblinstructors WHERE fldInstructorID = ?
    // Called by: SectionController and ScheduleService to fetch a specific instructor record
    Optional<Instructor> findByInstructorId(String instructorId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblinstructors WHERE fldInstructorID = ?
    // Called by: SectionController.updateInstructor() to verify the instructor exists before saving changes
    boolean existsByInstructorId(String instructorId);

    // Auto-generates: SELECT * FROM tblinstructors WHERE fldIsActive = 1
    // Called by: SectionController.getInstructors() to populate the instructor dropdown for schedule assignment
    List<Instructor> findByIsActiveTrue();
}

package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (SchoolYearRepository)
// Serves the SchoolYear entity — reads from and writes to the tblschoolyear table.
//
// Spring auto-generates SQL from the method names declared here:
//   findBySchoolYearId  → finds one school year by its business ID
//   findByIsActiveTrue  → returns the currently active school year (should be at most one)
//
// LAYER 4 → LAYER 5: Uses the SchoolYear entity to map database rows to objects.
// LAYER 4 → LAYER 2: SchoolYearController calls this to manage school year records.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SchoolYearRepository extends JpaRepository<SchoolYear, Integer> {

    // Auto-generates: SELECT * FROM tblschoolyear WHERE fldSchoolYearID = ?
    // Called by: SchoolYearController to fetch a specific school year record by its business ID
    Optional<SchoolYear> findBySchoolYearId(String schoolYearId);

    // Auto-generates: SELECT * FROM tblschoolyear WHERE fldIsActive = 1
    // Called by: SchoolYearController to identify the currently active school year (should be at most one)
    Optional<SchoolYear> findByIsActiveTrue();
}

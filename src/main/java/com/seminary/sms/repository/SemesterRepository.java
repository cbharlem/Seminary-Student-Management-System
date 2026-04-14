package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (SemesterRepository)
// Serves the Semester entity — reads from and writes to the tblsemester table.
//
// Spring auto-generates SQL from most method names. One uses a custom @Query:
//   findBySemesterId          → finds one semester by its business ID
//   findByIsActiveTrue        → returns the currently active semester (at most one)
//   findBySchoolYear_Index    → all semesters belonging to a given school year
//   findActiveWithSchoolYear  → custom @Query that also eagerly fetches the SchoolYear
//                               in one query (JOIN FETCH), avoiding a second DB call.
//                               This is used by the login page to show the active semester label.
//
// LAYER 4 → LAYER 5: Uses the Semester entity to map database rows to objects.
// LAYER 4 → LAYER 2: SchoolYearController and multiple other controllers use the active semester.
// LAYER 4 → LAYER 3: EnrollmentService uses this to find the current semester during enrollment.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface SemesterRepository extends JpaRepository<Semester, Integer> {

    // Auto-generates: SELECT * FROM tblsemester WHERE fldSemesterID = ?
    // Called by: SchoolYearController and EnrollmentService to fetch a specific semester by its business ID
    Optional<Semester> findBySemesterId(String semesterId);

    // Auto-generates: SELECT * FROM tblsemester WHERE fldIsActive = 1
    // Called by: multiple controllers to identify the currently active semester (at most one row is active)
    Optional<Semester> findByIsActiveTrue();

    // Auto-generates: JOIN tblschoolyear ON ... WHERE tblschoolyear.fldIndex = ?
    // Called by: SchoolYearController.getSemesters() to list all semesters under a given school year
    List<Semester> findBySchoolYear_Index(Integer schoolYearIndex);

    // Custom JPQL with JOIN FETCH — fetches the active semester AND its parent SchoolYear in one query
    // JOIN FETCH tells Hibernate to load the related SchoolYear immediately instead of making a second DB call
    // Called by: PublicController.getActiveSemester() to show the active semester label on the login page
    @Query("SELECT s FROM Semester s JOIN FETCH s.schoolYear WHERE s.isActive = true")
    Optional<Semester> findActiveWithSchoolYear();
}

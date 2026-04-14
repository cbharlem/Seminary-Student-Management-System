package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (CourseRepository)
// Serves the Course entity — reads from and writes to the tblcourses table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByCourseId                          → finds one course by its business ID
//   existsByCourseId                        → checks if a course ID already exists
//   findByIsActiveTrue                      → returns only active courses
//   findByProgram_Index                     → all courses for a program (by PK)
//   findByProgram_IndexAndIsActiveTrue      → active courses for a program (by PK)
//   findByProgram_ProgramIdAndIsActiveTrue  → active courses for a program (by business ID)
//
// LAYER 4 → LAYER 5: Uses the Course entity to map database rows to objects.
// LAYER 4 → LAYER 2: CurriculumController and EnrollmentController call this repository directly.
// LAYER 4 → LAYER 3: EnrollmentService also uses this to fetch course details.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CourseRepository extends JpaRepository<Course, Integer> {

    // Auto-generates: SELECT * FROM tblcourses WHERE fldCourseID = ?
    // Called by: CurriculumController, EnrollmentController, and EnrollmentService to fetch a specific course
    Optional<Course> findByCourseId(String courseId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblcourses WHERE fldCourseID = ?
    // Called by: duplicate-check logic before inserting a new course
    boolean existsByCourseId(String courseId);

    // Auto-generates: SELECT * FROM tblcourses WHERE fldIsActive = 1
    // Called by: CurriculumController and DashboardController to list/count active courses
    List<Course> findByIsActiveTrue();

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldIndex = ?
    // Called by: lookups that use the program's integer PK
    List<Course> findByProgram_Index(Integer programIndex);

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldIndex = ? AND fldIsActive = 1
    // Called by: enrollment subject dropdowns filtered by program PK
    List<Course> findByProgram_IndexAndIsActiveTrue(Integer programIndex);

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldProgramID = ? AND fldIsActive = 1
    // Called by: CurriculumController.getCourses() and EnrollmentController subject form
    List<Course> findByProgram_ProgramIdAndIsActiveTrue(String programId);
}

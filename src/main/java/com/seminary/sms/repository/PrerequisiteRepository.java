package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (PrerequisiteRepository)
// Serves the Prerequisite entity — reads from and writes to the tblprerequisites table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByCourse_Index      → all prerequisite rules for a given course (by PK)
//   findByCourse_CourseId   → all prerequisite rules for a given course (by business ID)
//   deleteByCourse_CourseId            → removes all prerequisites where this course is the main course
//   deleteByPrerequisiteCourse_CourseId → removes all rules where this course is the prerequisite
//                                         (both deletes are needed when a course is deleted)
//
// LAYER 4 → LAYER 5: Uses the Prerequisite entity to map database rows to objects.
// LAYER 4 → LAYER 3: EnrollmentService calls this to check if prerequisites are met.
// LAYER 4 → LAYER 2: CurriculumController calls this to add/remove prerequisite rules.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Integer> {

    // Auto-generates: JOIN tblcourses ON ... WHERE tblcourses.fldIndex = ?
    // Called by: lookups that fetch all prerequisite rules for a course (by PK)
    List<Prerequisite> findByCourse_Index(Integer courseIndex);

    // Auto-generates: JOIN tblcourses ON ... WHERE tblcourses.fldCourseID = ?
    // Called by: CurriculumController.getPrerequisites() to list what a course requires
    List<Prerequisite> findByCourse_CourseId(String courseId);

    // Auto-generates: DELETE FROM tblprerequisites WHERE tblcourses.fldCourseID = ? (main course side)
    // Called by: CurriculumController.deleteCourse() to remove all rules where this course is the main subject
    void deleteByCourse_CourseId(String courseId);

    // Auto-generates: DELETE FROM tblprerequisites WHERE prerequisite_course.fldCourseID = ?
    // Called by: CurriculumController.deleteCourse() to also remove rules where this course is the prerequisite
    // Both deletes together ensure no orphaned prerequisite rules remain after a course is removed
    void deleteByPrerequisiteCourse_CourseId(String courseId);
}

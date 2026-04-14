package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (GradeRepository)
// Serves the Grade entity — reads from and writes to the tblgrades table.
//
// Spring auto-generates SQL from most method names. One method uses a custom
// @Query annotation (written in JPQL — Java's version of SQL for entities):
//   findByGradeId                             → finds one grade by its business ID
//   findByStudent_Index/StudentId             → all grades for a student
//   findBySemester_Index/SemesterId           → all grades for a semester
//   findByStudent_...AndSemester_...          → grades for a student in a specific semester
//   findByEnrollmentSubject_Index             → the grade for one enrolled subject
//   findPassedGrade (@Query)                  → custom query: checks if a student passed
//                                               a specific course (used for prerequisite checks)
//
// LAYER 4 → LAYER 5: Uses the Grade entity to map database rows to objects.
// LAYER 4 → LAYER 3: GradeService and EnrollmentService call this to read and save grades.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface GradeRepository extends JpaRepository<Grade, Integer> {

    // Auto-generates: SELECT * FROM tblgrades WHERE fldGradeID = ?
    // Called by: GradeService and GradeController to fetch a specific grade record
    Optional<Grade> findByGradeId(String gradeId);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: GradeService to retrieve all grades for a student (by PK)
    List<Grade> findByStudent_Index(Integer studentIndex);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldStudentID = ?
    // Called by: GradeController.getByStudent() to list a student's complete grade history
    List<Grade> findByStudent_StudentId(String studentId);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldIndex = ?
    // Called by: lookups that fetch all grades recorded in a semester (by PK)
    List<Grade> findBySemester_Index(Integer semesterIndex);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldSemesterID = ?
    // Called by: GradeController.getBySemester() to list all grades for a given semester
    List<Grade> findBySemester_SemesterId(String semesterId);

    // Auto-generates: JOIN tblstudents ... JOIN tblsemester ... WHERE fldIndex = ? AND fldIndex = ?
    // Called by: GradeService to get a student's grades for one specific semester (by PKs)
    List<Grade> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);

    // Auto-generates: JOIN tblstudents ... JOIN tblsemester ... WHERE fldStudentID = ? AND fldSemesterID = ?
    // Called by: StudentMeController.getMyGrades() to show a student their own grades for a semester
    List<Grade> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);

    // Auto-generates: JOIN tblenrollmentsubjects ON ... WHERE tblenrollmentsubjects.fldIndex = ?
    // Called by: GradeService to find the grade linked to a specific enrolled subject
    Optional<Grade> findByEnrollmentSubject_Index(Integer enrollmentSubjectIndex);

    // Custom JPQL query — too complex for method-name auto-generation
    // Finds a grade where the student passed a specific course (used to verify prerequisites are met)
    // Called by: EnrollmentService.checkPrerequisites() before allowing a student to enroll in a course
    @Query("SELECT g FROM Grade g WHERE g.student.studentId = :studentId AND g.course.courseId = :courseId AND g.gradeStatus = :status")
    Optional<Grade> findPassedGrade(@Param("studentId") String studentId, @Param("courseId") String courseId, @Param("status") Grade.GradeStatus status);
}

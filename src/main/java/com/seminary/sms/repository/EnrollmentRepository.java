package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (EnrollmentRepository)
// Serves the Enrollment entity — reads from and writes to the tblenrollment table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByEnrollmentId                                  → finds one enrollment by business ID
//   findByStudent_IndexAndSemester_Index                → checks if a student is enrolled in a semester (by PKs)
//   findByStudent_StudentIdAndSemester_SemesterId       → same check using business IDs
//   findBySemester_Index / findBySemester_SemesterId    → all enrollments for a semester
//   countBySemester_SemesterId                          → how many students are enrolled this semester
//   findByStudent_Index / findByStudent_StudentId       → all enrollments for one student
//
// LAYER 4 → LAYER 5: Uses the Enrollment entity to map database rows to objects.
// LAYER 4 → LAYER 3: EnrollmentService calls this to check, create, and retrieve enrollments.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

    // Auto-generates: SELECT * FROM tblenrollment WHERE fldEnrollmentID = ?
    // Called by: EnrollmentService and EnrollmentController to fetch a specific enrollment record
    Optional<Enrollment> findByEnrollmentId(String enrollmentId);

    // Auto-generates: JOIN tblstudents ON ... JOIN tblsemester ON ... WHERE fldIndex = ? AND fldIndex = ?
    // Called by: EnrollmentService.enroll() to check if a student is already enrolled this semester (using PKs)
    Optional<Enrollment> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);

    // Auto-generates: JOIN tblstudents ON ... JOIN tblsemester ON ... WHERE fldStudentID = ? AND fldSemesterID = ?
    // Called by: EnrollmentService.enroll() to check for duplicate enrollment using business IDs
    Optional<Enrollment> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldIndex = ?
    // Called by: lookups that fetch all enrollments for a semester using the integer PK
    List<Enrollment> findBySemester_Index(Integer semesterIndex);

    // Auto-generates: JOIN tblsemester ON ... WHERE tblsemester.fldSemesterID = ?
    // Called by: EnrollmentController to list all enrolled students for a given semester
    List<Enrollment> findBySemester_SemesterId(String semesterId);

    // Auto-generates: SELECT COUNT(*) FROM tblenrollment JOIN tblsemester ON ... WHERE fldSemesterID = ?
    // Called by: DashboardController to count how many students are enrolled in the current semester
    long countBySemester_SemesterId(String semesterId);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: lookups that retrieve all enrollments for a student using the integer PK
    List<Enrollment> findByStudent_Index(Integer studentIndex);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldStudentID = ?
    // Called by: EnrollmentController.getByStudent() to list a student's full enrollment history
    List<Enrollment> findByStudent_StudentId(String studentId);
}

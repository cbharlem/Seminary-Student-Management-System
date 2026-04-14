package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (EnrollmentSubjectRepository)
// Serves the EnrollmentSubject entity — reads from and writes to the tblenrollmentsubjects table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByEnrollment_Index         → all subjects for a given enrollment (by PK)
//   findByEnrollment_EnrollmentId  → all subjects for a given enrollment (by business ID)
//   findByEnrollment_Student_Index → all subjects enrolled by a student across all semesters
//                                    (navigates: EnrollmentSubject → Enrollment → Student)
//
// LAYER 4 → LAYER 5: Uses the EnrollmentSubject entity to map database rows to objects.
// LAYER 4 → LAYER 3: EnrollmentService and GradeService call this to manage enrolled subjects.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.EnrollmentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EnrollmentSubjectRepository extends JpaRepository<EnrollmentSubject, Integer> {

    // Auto-generates: JOIN tblenrollment ON ... WHERE tblenrollment.fldIndex = ?
    // Called by: EnrollmentService and GradeService to fetch the subjects in an enrollment (by PK)
    List<EnrollmentSubject> findByEnrollment_Index(Integer enrollmentIndex);

    // Auto-generates: JOIN tblenrollment ON ... WHERE tblenrollment.fldEnrollmentID = ?
    // Called by: EnrollmentController.getSubjects() to list enrolled subjects for a given enrollment
    List<EnrollmentSubject> findByEnrollment_EnrollmentId(String enrollmentId);

    // Auto-generates: JOIN tblenrollment ON ... JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: GradeService to get all subjects a student has ever enrolled in across all semesters
    List<EnrollmentSubject> findByEnrollment_Student_Index(Integer studentIndex);
}

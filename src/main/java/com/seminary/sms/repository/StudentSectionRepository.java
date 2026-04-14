package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (StudentSectionRepository)
// Serves the StudentSection entity — reads from and writes to the tblstudentsection table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByStudent_IndexAndSemester_Index              → finds a student's section assignment by PKs
//   findByStudent_StudentIdAndSemester_SemesterId     → same lookup using business IDs
//   findByStudent_Index                               → all section assignments for a student
//   findBySection_Index                               → all students assigned to a section
//
// LAYER 4 → LAYER 5: Uses the StudentSection entity to map database rows to objects.
// LAYER 4 → LAYER 3: EnrollmentService creates StudentSection records; ScheduleMeController
//                     reads them to find a student's section for their personal schedule.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.StudentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface StudentSectionRepository extends JpaRepository<StudentSection, Integer> {

    // Auto-generates: JOIN tblstudents ... JOIN tblsemester ... WHERE fldIndex = ? AND fldIndex = ?
    // Called by: EnrollmentService to check if a student already has a section assigned this semester (by PKs)
    Optional<StudentSection> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);

    // Auto-generates: JOIN tblstudents ... JOIN tblsemester ... WHERE fldStudentID = ? AND fldSemesterID = ?
    // Called by: StudentMeController.getMySchedule() to find the student's section for the current semester
    Optional<StudentSection> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: lookups that retrieve all section assignments for a student across semesters
    List<StudentSection> findByStudent_Index(Integer studentIndex);

    // Auto-generates: JOIN tblsection ON ... WHERE tblsection.fldIndex = ?
    // Called by: SectionController to list all students currently assigned to a section
    List<StudentSection> findBySection_Index(Integer sectionIndex);
}

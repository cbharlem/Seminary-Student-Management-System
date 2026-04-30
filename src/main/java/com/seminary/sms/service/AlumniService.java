package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (AlumniService)
// Handles all business logic related to alumni records.
//
// This service sits between the controller (Layer 2) and the repositories (Layer 4).
// Controllers should never talk directly to repositories for complex operations —
// they ask the service instead, and the service decides what to do.
//
// Repositories used:
//   AlumniRepository            — to save and retrieve alumni records
//   StudentRepository           — to look up the student being graduated and update their status
//   CourseRepository            — to fetch all required courses from the student's active curriculum
//   GradeRepository             — to check which required courses the student has already passed
//   CurriculumVersionRepository — to find the active curriculum for the student's program
//
// Business logic handled here:
//   - graduateStudent()           → verifies the student has passed all curriculum courses (Year 1–4),
//                                   then changes status to Alumni and creates an Alumni record.
//                                   Uses @Transactional so both saves either succeed together or both fail.
//   - checkGraduationEligibility() → returns the list of course names the student has not yet passed.
//                                    Empty list means the student is eligible to graduate.
//   - unmarkAlumni()              → reverses the graduation: deletes the Alumni record, then
//                                   sets the student's status back to Active.
//   - update()                    → saves changes to an existing alumni record.
//
// LAYER 3 → LAYER 4: Calls the four repositories to read/write the database.
// LAYER 4 → LAYER 3: Repositories return entity objects.
// LAYER 3 → LAYER 2: AlumniController calls this service and sends the result to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class AlumniService {

    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final GradeRepository gradeRepository;
    private final CurriculumVersionRepository curriculumVersionRepository;

    // LAYER 2 → LAYER 3: Called by AlumniController.getAll() to list all alumni records
    // LAYER 3 → LAYER 4: Calls alumniRepository.findAll() — returns every row in tblalumni
    public List<Alumni> getAll() {
        return alumniRepository.findAll();
    }

    // LAYER 2 → LAYER 3: Utility method to look up an alumni record by the linked student's ID
    // LAYER 3 → LAYER 4: Calls alumniRepository.findByStudent_StudentId() to find the matching row
    public Optional<Alumni> getByStudentId(String studentId) {
        return alumniRepository.findByStudent_StudentId(studentId);
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.checkEligibility() before opening the graduate modal
    // LAYER 3 → LAYER 4: Fetches all required courses from the active curriculum, then checks grades
    // Returns incomplete courses as maps with courseName, yearLevel, semesterNumber — sorted by year then semester then name.
    // Empty list = student is eligible to graduate.
    public List<Map<String, Object>> checkGraduationEligibility(String studentId) {
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        String programId = student.getProgram().getProgramId();

        // Get all active courses from the student's program's active curriculum
        List<Course> requiredCourses = curriculumVersionRepository
            .findByProgram_ProgramIdAndIsActiveTrue(programId)
            .map(curriculum -> courseRepository.findByCurriculum_CurriculumIdAndIsActiveTrue(curriculum.getCurriculumId()))
            .orElseGet(() -> courseRepository.findByProgram_ProgramIdAndIsActiveTrue(programId));

        // Collect courseIds the student has already passed
        Set<String> passedCourseIds = gradeRepository
            .findByStudent_StudentIdAndGradeStatus(studentId, Grade.GradeStatus.Passed)
            .stream()
            .map(g -> g.getCourse().getCourseId())
            .collect(Collectors.toSet());

        // Return incomplete courses sorted by year → semester → name so the frontend can group them
        return requiredCourses.stream()
            .filter(c -> !passedCourseIds.contains(c.getCourseId()))
            .sorted(Comparator.comparingInt(Course::getYearLevel)
                .thenComparingInt(Course::getSemesterNumber)
                .thenComparing(Course::getCourseName))
            .map(c -> Map.<String, Object>of(
                "courseName",     c.getCourseName(),
                "yearLevel",      c.getYearLevel(),
                "semesterNumber", c.getSemesterNumber()
            ))
            .collect(Collectors.toList());
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.graduate() when the registrar graduates a student
    // LAYER 3 → LAYER 4: Verifies curriculum completion, then changes status to Alumni and creates an Alumni record
    // @Transactional: if either save fails, both changes are rolled back to keep data consistent
    @Transactional
    public Alumni graduateStudent(String studentId, LocalDate graduationDate, String honors) {
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        if (student.getCurrentStatus() == Student.StudentStatus.Alumni) {
            throw new RuntimeException("Student is already an alumnus.");
        }

        // Block graduation if any required courses are not yet passed
        List<Map<String, Object>> incomplete = checkGraduationEligibility(studentId);
        if (!incomplete.isEmpty()) {
            String names = incomplete.stream()
                .map(m -> (String) m.get("courseName"))
                .collect(Collectors.joining(", "));
            throw new RuntimeException("Student has not passed all required courses. Incomplete: " + names);
        }

        student.setCurrentStatus(Student.StudentStatus.Alumni);
        studentRepository.save(student);

        Alumni alumni = Alumni.builder()
            .alumniId("ALM-" + System.currentTimeMillis())
            .student(student)
            .program(student.getProgram())
            .graduationDate(graduationDate)
            .yearGraduated(String.valueOf(graduationDate.getYear()))
            .honors(honors)
            .build();
        return alumniRepository.save(alumni);
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.update() when the registrar edits alumni details
    // LAYER 3 → LAYER 4: Calls alumniRepository.save() to persist the updated Alumni object
    @Transactional
    public Alumni update(Alumni alumni) {
        return alumniRepository.save(alumni);
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.unmarkAlumni() to reverse a graduation
    // LAYER 3 → LAYER 4: Deletes the Alumni record first (to release the FK constraint),
    //   then reactivates the student by setting their status back to Active — both in one transaction
    @Transactional
    public void unmarkAlumni(String alumniId) {
        Alumni alumni = alumniRepository.findByAlumniId(alumniId)
            .orElseThrow(() -> new RuntimeException("Alumni record not found: " + alumniId));
        // Capture student reference and PK before deleting alumni
        Student student = alumni.getStudent();
        Integer alumniPk = alumni.getIndex();
        // Delete alumni record first to release the FK constraint on student
        alumniRepository.deleteById(alumniPk);
        alumniRepository.flush();
        // Then revert student status
        if (student != null) {
            student.setCurrentStatus(Student.StudentStatus.Active);
            studentRepository.save(student);
        }
    }
}

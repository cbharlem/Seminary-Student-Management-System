package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (EnrollmentService)
// Handles all business logic for enrolling students into semesters and subjects.
//
// Repositories used:
//   EnrollmentRepository        — to create and look up enrollment records
//   EnrollmentSubjectRepository — to add individual subjects to an enrollment
//   PrerequisiteRepository      — to check if a student has met prerequisites
//   GradeRepository             — to verify whether prerequisite courses were passed
//   SemesterRepository          — to find the active semester
//   StudentSectionRepository    — to assign a student to a section during enrollment
//   CourseRepository            — to fetch full course details when enrolling a subject
//
// Business logic handled here:
//   - enroll()             → creates an Enrollment record and (optionally) assigns a section.
//                            Prevents duplicate enrollment in the same semester.
//   - enrollSubject()      → adds one subject to an existing enrollment after checking prerequisites.
//   - checkPrerequisites() → looks up prerequisite rules and checks the student's grade history.
//                            Returns a list of unmet requirements (empty = all clear).
//   - getActiveEnrollment()→ finds the student's enrollment for the currently active semester.
//
// LAYER 3 → LAYER 4: Calls all seven repositories above to read/write the database.
// LAYER 4 → LAYER 3: Repositories return Enrollment, EnrollmentSubject, and related objects.
// LAYER 3 → LAYER 2: EnrollmentController calls this service and returns results to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSubjectRepository enrollmentSubjectRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final GradeRepository gradeRepository;
    private final SemesterRepository semesterRepository;
    private final StudentSectionRepository studentSectionRepository;
    private final CourseRepository courseRepository;

    // LAYER 2 → LAYER 3: Called by EnrollmentController.getByStudent() to show a student's enrollment history
    // LAYER 3 → LAYER 4: Calls enrollmentRepository.findByStudent_StudentId()
    public List<Enrollment> getEnrollmentsByStudent(String studentId) {
        return enrollmentRepository.findByStudent_StudentId(studentId);
    }

    // LAYER 2 → LAYER 3: Utility method — finds the enrollment for a student in the currently active semester
    // LAYER 3 → LAYER 4: First finds the active semester, then queries for the matching enrollment record
    public Optional<Enrollment> getActiveEnrollment(String studentId) {
        Semester active = semesterRepository.findByIsActiveTrue().orElse(null);
        if (active == null) return Optional.empty();
        return enrollmentRepository.findByStudent_StudentIdAndSemester_SemesterId(studentId, active.getSemesterId());
    }

    // LAYER 2 → LAYER 3: Called by EnrollmentController.getAll() when a semester filter is provided
    // LAYER 3 → LAYER 4: Calls enrollmentRepository.findBySemester_SemesterId()
    public List<Enrollment> getBySemester(String semesterId) {
        return enrollmentRepository.findBySemester_SemesterId(semesterId);
    }

    // LAYER 2 → LAYER 3: Utility method — counts how many students are enrolled in a given semester
    // LAYER 3 → LAYER 4: Calls enrollmentRepository.countBySemester_SemesterId()
    public long countBySemester(String semesterId) {
        return enrollmentRepository.countBySemester_SemesterId(semesterId);
    }

    // LAYER 2 → LAYER 3: Called by EnrollmentController.enroll() after resolving all references
    // LAYER 3 → LAYER 4: Checks for duplicate enrollment, creates an Enrollment record, and optionally
    //   creates a StudentSection assignment — all in one transaction
    // @Transactional: if any save fails, neither the enrollment nor the section assignment is committed
    @Transactional
    public Enrollment enroll(Student student, Program program, Semester semester, Section section, Integer yearLevel) {
        // Check if already enrolled
        if (enrollmentRepository.findByStudent_StudentIdAndSemester_SemesterId(
                student.getStudentId(), semester.getSemesterId()).isPresent()) {
            throw new RuntimeException("Student is already enrolled this semester.");
        }

        String enrollmentId = "ENR-" + String.format("%03d", 1 + enrollmentRepository.count());
        Enrollment enrollment = Enrollment.builder()
            .enrollmentId(enrollmentId)
            .student(student)
            .program(program)
            .semester(semester)
            .yearLevel(yearLevel)
            .enrollmentDate(LocalDate.now())
            .enrollmentStatus(Enrollment.EnrollmentStatus.Enrolled)
            .build();
        enrollment = enrollmentRepository.save(enrollment);

        // Assign section
        if (section != null) {
            StudentSection ss = StudentSection.builder()
                .studentSectionId("SS-" + String.format("%03d", 1 + studentSectionRepository.count()))
                .student(student)
                .section(section)
                .semester(semester)
                .dateAssigned(LocalDate.now())
                .build();
            studentSectionRepository.save(ss);
        }
        return enrollment;
    }

    /**
     * Check if student has passed all prerequisites for a course.
     * Returns list of unmet prerequisite course names (empty = all met).
     */
    public List<String> checkPrerequisites(String studentId, String courseId) {
        List<Prerequisite> prereqs = prerequisiteRepository.findByCourse_CourseId(courseId);
        List<String> unmet = new ArrayList<>();
        for (Prerequisite p : prereqs) {
            String prereqCourseId = p.getPrerequisiteCourse().getCourseId();
            Optional<Grade> passed = gradeRepository.findPassedGrade(studentId, prereqCourseId, Grade.GradeStatus.Passed);
            if (passed.isEmpty()) {
                unmet.add(p.getPrerequisiteCourse().getCourseName() + " (" + p.getPrerequisiteCourse().getCourseCode() + ")");
            }
        }
        return unmet;
    }

    // LAYER 2 → LAYER 3: Called by EnrollmentController.enrollSubject() to add one subject to an enrollment
    // LAYER 3 → LAYER 4: Fetches the full Course from DB, runs checkPrerequisites(), then saves an EnrollmentSubject
    // Throws a RuntimeException if any prerequisite course has not been passed yet
    @Transactional
    public EnrollmentSubject enrollSubject(Enrollment enrollment, Course courseRef, Schedule schedule) {
        // Fetch full course from DB
        Course course = courseRepository.findByCourseId(courseRef.getCourseId())
            .orElseThrow(() -> new RuntimeException("Course not found: " + courseRef.getCourseId()));

        // Check prerequisites
        List<String> unmet = checkPrerequisites(enrollment.getStudent().getStudentId(), course.getCourseId());
        if (!unmet.isEmpty()) {
            throw new RuntimeException("Prerequisites not met: " + String.join(", ", unmet));
        }

        EnrollmentSubject es = EnrollmentSubject.builder()
            .enrollmentSubjectId("ES-" + System.currentTimeMillis())
            .enrollment(enrollment)
            .course(course)
            .schedule(schedule)
            .status(EnrollmentSubject.SubjectStatus.Enrolled)
            .build();
        return enrollmentSubjectRepository.save(es);
    }

    // LAYER 2 → LAYER 3: Called by EnrollmentController.getSubjects() to list the subjects in an enrollment
    // LAYER 3 → LAYER 4: Calls enrollmentSubjectRepository.findByEnrollment_EnrollmentId()
    public List<EnrollmentSubject> getSubjectsByEnrollment(String enrollmentId) {
        return enrollmentSubjectRepository.findByEnrollment_EnrollmentId(enrollmentId);
    }
}

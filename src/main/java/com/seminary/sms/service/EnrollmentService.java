package com.seminary.sms.service;

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

    public List<Enrollment> getEnrollmentsByStudent(String studentId) {
        return enrollmentRepository.findByStudent_StudentId(studentId);
    }

    public Optional<Enrollment> getActiveEnrollment(String studentId) {
        Semester active = semesterRepository.findByIsActiveTrue().orElse(null);
        if (active == null) return Optional.empty();
        return enrollmentRepository.findByStudent_StudentIdAndSemester_SemesterId(studentId, active.getSemesterId());
    }

    public List<Enrollment> getBySemester(String semesterId) {
        return enrollmentRepository.findBySemester_SemesterId(semesterId);
    }

    public long countBySemester(String semesterId) {
        return enrollmentRepository.countBySemester_SemesterId(semesterId);
    }

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

    public List<EnrollmentSubject> getSubjectsByEnrollment(String enrollmentId) {
        return enrollmentSubjectRepository.findByEnrollment_EnrollmentId(enrollmentId);
    }
}

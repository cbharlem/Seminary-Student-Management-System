package com.seminary.sms.controller;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import com.seminary.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeParseException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

// ── Enrollment ────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/enrollment")
@RequiredArgsConstructor
class EnrollmentController {

    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentService enrollmentService;
    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;
    private final SectionRepository sectionRepository;
    private final CourseRepository courseRepository;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Enrollment> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return enrollmentService.getBySemester(semester);
        return enrollmentRepository.findAll();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Enrollment> getByStudent(@PathVariable String studentId) {
        return enrollmentService.getEnrollmentsByStudent(studentId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        try {
            Student student = studentRepository.findByStudentId(body.get("studentId"))
                .orElseThrow(() -> new RuntimeException("Student not found"));
            Program program = programRepository.findByProgramId(body.get("programId"))
                .orElseThrow(() -> new RuntimeException("Program not found"));
            Semester semester = semesterRepository.findBySemesterId(body.get("semesterId"))
                .orElseThrow(() -> new RuntimeException("Semester not found"));
            Section section = body.containsKey("sectionId")
                ? sectionRepository.findBySectionId(body.get("sectionId")).orElse(null) : null;
            // SECURITY (A07): Validate yearLevel bounds to prevent corrupt data
            int yearLevel = Integer.parseInt(body.getOrDefault("yearLevel", "1"));
            if (yearLevel < 1 || yearLevel > 10)
                throw new RuntimeException("Year level must be between 1 and 10.");
            return ResponseEntity.ok(enrollmentService.enroll(student, program, semester, section, yearLevel));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @GetMapping("/{enrollmentId}/subjects")
    @PreAuthorize("hasRole('Registrar')")
    public List<EnrollmentSubject> getSubjects(@PathVariable String enrollmentId) {
        return enrollmentService.getSubjectsByEnrollment(enrollmentId);
    }

    @PostMapping("/{enrollmentId}/subjects")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> enrollSubject(@PathVariable String enrollmentId,
                                            @RequestBody Map<String, String> body) {
        try {
            Enrollment enrollment = enrollmentRepository.findByEnrollmentId(enrollmentId)
                .orElseThrow(() -> new RuntimeException("Enrollment not found"));
            // SECURITY (A07): Fetch course from DB instead of constructing in-memory — prevents enrolling in non-existent courses
            Course course = courseRepository.findByCourseId(body.get("courseId"))
                .orElseThrow(() -> new RuntimeException("Course not found"));
            return ResponseEntity.ok(enrollmentService.enrollSubject(enrollment, course, null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }
}

// ── Grades ────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
class GradeController {

    private final GradeRepository gradeRepository;
    private final GradeService gradeService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Grade> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return gradeService.getGradesBySemester(semester);
        return gradeRepository.findAll();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Grade> getByStudent(@PathVariable String studentId,
                                     @RequestParam(required = false) String semester) {
        if (semester != null) return gradeService.getGradesByStudentAndSemester(studentId, semester);
        return gradeService.getGradesByStudent(studentId);
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> save(@RequestBody Grade grade, Authentication auth) {
        try {
            User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            return ResponseEntity.ok(gradeService.saveGrade(grade, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> update(@PathVariable Integer id,
                                     @RequestBody Map<String, Object> body,
                                     Authentication auth) {
        try {
            User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            BigDecimal midterm = body.get("midtermGrade") != null
                ? new BigDecimal(body.get("midtermGrade").toString()) : null;
            BigDecimal finalG = body.get("finalGrade") != null
                ? new BigDecimal(body.get("finalGrade").toString()) : null;
            // SECURITY (A04): Validate grade range — Philippine grading: 1.0 (highest) to 5.0 (failed)
            BigDecimal MIN_GRADE = new BigDecimal("1.0");
            BigDecimal MAX_GRADE = new BigDecimal("5.0");
            if (midterm != null && (midterm.compareTo(MIN_GRADE) < 0 || midterm.compareTo(MAX_GRADE) > 0))
                return ResponseEntity.badRequest().body(Map.of("error", "Midterm grade must be between 1.0 and 5.0"));
            if (finalG != null && (finalG.compareTo(MIN_GRADE) < 0 || finalG.compareTo(MAX_GRADE) > 0))
                return ResponseEntity.badRequest().body(Map.of("error", "Final grade must be between 1.0 and 5.0"));
            // SECURITY (A07): Null check before toString — and enum poisoning catch below
            if (body.get("gradeStatus") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Grade status is required."));
            Grade.GradeStatus status = Grade.GradeStatus.valueOf(body.get("gradeStatus").toString());
            String remarks = body.get("remarks") != null ? body.get("remarks").toString() : null;
            return ResponseEntity.ok(gradeService.updateGrade(id, midterm, finalG, status, remarks, user));
        } catch (IllegalArgumentException e) {
            // SECURITY (A08): Enum poisoning — return generic message not class path
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid grade status value."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }
}

// ── Schedule ──────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
class ScheduleController {

    private final ScheduleRepository scheduleRepository;
    private final ScheduleService scheduleService;
    private final SectionRepository sectionRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;
    private final CourseRepository courseRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Schedule> getAll(@RequestParam(required = false) String section) {
        if (section != null) return scheduleService.getBySection(section);
        return scheduleRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            Schedule schedule = new Schedule();
            schedule.setScheduleId("SCH-" + System.currentTimeMillis());
            // SECURITY (A07): Require section and course — prevent schedules with null references
            schedule.setSection(sectionRepository.findBySectionId(body.get("sectionId"))
                .orElseThrow(() -> new RuntimeException("Section not found")));
            schedule.setCourse(courseRepository.findByCourseId(body.get("courseId"))
                .orElseThrow(() -> new RuntimeException("Course not found")));
            instructorRepository.findByInstructorId(body.get("instructorId")).ifPresent(schedule::setInstructor);
            roomRepository.findByRoomId(body.get("roomId")).ifPresent(schedule::setRoom);
            // SECURITY (A08): Enum poisoning catch for DayOfWeek handled below
            if (body.get("dayOfWeek") == null)
                throw new RuntimeException("Day of week is required.");
            schedule.setDayOfWeek(Schedule.DayOfWeek.valueOf(body.get("dayOfWeek")));
            // SECURITY (A07): Null checks before LocalTime.parse
            if (body.get("timeStart") == null || body.get("timeEnd") == null)
                throw new RuntimeException("Start time and end time are required.");
            schedule.setTimeStart(java.time.LocalTime.parse(body.get("timeStart")));
            schedule.setTimeEnd(java.time.LocalTime.parse(body.get("timeEnd")));
            return ResponseEntity.ok(scheduleService.save(schedule));
        } catch (IllegalArgumentException e) {
            // SECURITY (A08): Enum poisoning — return generic message not class path
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid day of week value."));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid time format. Use HH:mm (e.g. 08:00)."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Schedule deleted"));
    }
}

package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (EnrollmentGradeScheduleControllers.java)
// This file bundles three controllers that handle the core academic operations:
//
//   EnrollmentController  → @RequestMapping("/api/enrollment")
//      Handles enrolling students in semesters and adding subjects to enrollments.
//      Delegates complex logic to EnrollmentService.
//      Also uses: StudentRepository, ProgramRepository, SemesterRepository,
//                 SectionRepository, CourseRepository, EnrollmentRepository
//
//   GradeController       → @RequestMapping("/api/grades")
//      Handles viewing, saving, and updating student grades.
//      Delegates to GradeService for computation and saving.
//      Also uses: GradeRepository, UserRepository (to identify who entered the grade)
//
//   ScheduleController    → @RequestMapping("/api/schedule")
//      Handles creating, updating, and deleting class schedules.
//      Delegates conflict detection and saving to ScheduleService.
//      Also uses: ScheduleRepository, SectionRepository, InstructorRepository,
//                 RoomRepository, CourseRepository
//
// LAYER 2 → LAYER 3: All three controllers delegate business logic to their respective services.
// LAYER 2 → LAYER 4: Also calls repositories directly for simple lookups (e.g., find by ID).
// LAYER 2 → LAYER 1: Returns JSON responses that the frontend (app.js) uses to update the UI.
// ─────────────────────────────────────────────────────────────────────────────

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

    // LAYER 1 → LAYER 2: Triggered by app.js loadEnrollment() to populate the enrollment table
    // LAYER 2 → LAYER 3: Delegates to enrollmentService.getBySemester() if a semester filter is provided
    // LAYER 2 → LAYER 1: Returns a JSON list of Enrollment records matching the filter
    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Enrollment> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return enrollmentService.getBySemester(semester);
        return enrollmentRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js viewStudent() to load enrollment history in the student detail panel
    // LAYER 2 → LAYER 3: Delegates to enrollmentService.getEnrollmentsByStudent()
    // LAYER 2 → LAYER 1: Returns a JSON list of all enrollments for this student across all semesters
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Enrollment> getByStudent(@PathVariable String studentId) {
        return enrollmentService.getEnrollmentsByStudent(studentId);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveEnrollment() when the registrar enrolls a student
    // LAYER 2 → LAYER 3: Resolves all references (student, program, semester, section) then delegates to enrollmentService.enroll()
    // LAYER 2 → LAYER 1: Returns the new Enrollment JSON, or 400 if already enrolled or year level is out of range
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

    // LAYER 1 → LAYER 2: Triggered by app.js refreshSubjectsTable() to load the subjects in an enrollment
    // LAYER 2 → LAYER 3: Delegates to enrollmentService.getSubjectsByEnrollment()
    // LAYER 2 → LAYER 1: Returns a JSON list of EnrollmentSubject objects for this enrollment
    @GetMapping("/{enrollmentId}/subjects")
    @PreAuthorize("hasRole('Registrar')")
    public List<EnrollmentSubject> getSubjects(@PathVariable String enrollmentId) {
        return enrollmentService.getSubjectsByEnrollment(enrollmentId);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js addEnrollmentSubject() when the registrar adds a subject to an enrollment
    // LAYER 2 → LAYER 3: Resolves the enrollment and course, then delegates to enrollmentService.enrollSubject()
    //   which also checks prerequisites before saving
    // LAYER 2 → LAYER 1: Returns the new EnrollmentSubject JSON, or 400 if prerequisites are not met
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

    // LAYER 1 → LAYER 2: Triggered by app.js loadGrades() to populate the grades management table
    // LAYER 2 → LAYER 3: Delegates to gradeService.getGradesBySemester() if filtered, otherwise returns all
    // LAYER 2 → LAYER 1: Returns a JSON list of Grade records
    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Grade> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return gradeService.getGradesBySemester(semester);
        return gradeRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js viewStudent() to show a student's grades in the detail panel
    // LAYER 2 → LAYER 3: Delegates to gradeService filtered by student and optionally by semester
    // LAYER 2 → LAYER 1: Returns a JSON list of Grade records for the specified student
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Grade> getByStudent(@PathVariable String studentId,
                                     @RequestParam(required = false) String semester) {
        if (semester != null) return gradeService.getGradesByStudentAndSemester(studentId, semester);
        return gradeService.getGradesByStudent(studentId);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js when a new grade record is first created
    // LAYER 2 → LAYER 3: Identifies the logged-in user, then delegates to gradeService.saveGrade()
    //   which assigns the grade ID, computes the final rating, and syncs the enrollment subject status
    // LAYER 2 → LAYER 1: Returns the saved Grade JSON
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

    // LAYER 1 → LAYER 2: Triggered by app.js saveGrade() when the registrar edits an existing grade
    // LAYER 2 → LAYER 3: Validates midterm/final grade ranges, validates the status enum, then delegates to gradeService.updateGrade()
    // LAYER 2 → LAYER 1: Returns the updated Grade JSON, or 400 if grades are out of range or status is invalid
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> update(@PathVariable String id,
                                     @RequestBody Map<String, Object> body,
                                     Authentication auth) {
        try {
            User user = userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            BigDecimal mtCS   = body.get("midtermClassStanding") != null ? new BigDecimal(body.get("midtermClassStanding").toString()) : null;
            BigDecimal mtExam = body.get("midtermExam")          != null ? new BigDecimal(body.get("midtermExam").toString())          : null;
            BigDecimal fnCS   = body.get("finalClassStanding")   != null ? new BigDecimal(body.get("finalClassStanding").toString())   : null;
            BigDecimal fnExam = body.get("finalExam")            != null ? new BigDecimal(body.get("finalExam").toString())            : null;
            // SECURITY (A04): Validate grade range — Philippine grading: 1.0 (highest) to 5.0 (failed)
            BigDecimal MIN = new BigDecimal("1.0"), MAX = new BigDecimal("5.0");
            if (mtCS   != null && (mtCS.compareTo(MIN)   < 0 || mtCS.compareTo(MAX)   > 0)) return ResponseEntity.badRequest().body(Map.of("error", "Midterm class standing must be between 1.0 and 5.0"));
            if (mtExam != null && (mtExam.compareTo(MIN) < 0 || mtExam.compareTo(MAX) > 0)) return ResponseEntity.badRequest().body(Map.of("error", "Midterm exam grade must be between 1.0 and 5.0"));
            if (fnCS   != null && (fnCS.compareTo(MIN)   < 0 || fnCS.compareTo(MAX)   > 0)) return ResponseEntity.badRequest().body(Map.of("error", "Final class standing must be between 1.0 and 5.0"));
            if (fnExam != null && (fnExam.compareTo(MIN) < 0 || fnExam.compareTo(MAX) > 0)) return ResponseEntity.badRequest().body(Map.of("error", "Final exam grade must be between 1.0 and 5.0"));
            // SECURITY (A07): Null check before toString — and enum poisoning catch below
            if (body.get("gradeStatus") == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Grade status is required."));
            Grade.GradeStatus status = Grade.GradeStatus.valueOf(body.get("gradeStatus").toString());
            String remarks = body.get("remarks") != null ? body.get("remarks").toString() : null;
            return ResponseEntity.ok(gradeService.updateGrade(id, mtCS, mtExam, fnCS, fnExam, status, remarks, user));
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

    // LAYER 1 → LAYER 2: Triggered by app.js loadSchedule() and loadScheduleGrid() to show class schedules
    // LAYER 2 → LAYER 3: Delegates to scheduleService.getBySection() if filtered, otherwise returns all
    // LAYER 2 → LAYER 1: Returns a JSON list of Schedule objects
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Schedule> getAll(@RequestParam(required = false) String section) {
        if (section != null) return scheduleService.getBySection(section);
        return scheduleRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveSchedule() when creating a new class schedule
    // LAYER 2 → LAYER 3: Builds the Schedule object from the request body, then delegates to scheduleService.save()
    //   which runs conflict detection before saving
    // LAYER 2 → LAYER 1: Returns the saved Schedule JSON, or 400 if there is a room or instructor conflict
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

    // LAYER 1 → LAYER 2: Triggered by app.js saveSchedule() when editing an existing class schedule
    // LAYER 2 → LAYER 3: Rebuilds the Schedule from the request body, then delegates to scheduleService.update()
    //   which re-runs conflict detection excluding the current schedule from the check
    // LAYER 2 → LAYER 1: Returns the updated Schedule JSON, or 400 if a conflict is detected
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Schedule schedule = scheduleRepository.findByScheduleId(id)
                .orElseThrow(() -> new RuntimeException("Schedule not found"));
            schedule.setSection(sectionRepository.findBySectionId(body.get("sectionId"))
                .orElseThrow(() -> new RuntimeException("Section not found")));
            schedule.setCourse(courseRepository.findByCourseId(body.get("courseId"))
                .orElseThrow(() -> new RuntimeException("Course not found")));
            instructorRepository.findByInstructorId(body.get("instructorId")).ifPresent(schedule::setInstructor);
            roomRepository.findByRoomId(body.get("roomId")).ifPresent(schedule::setRoom);
            if (body.get("dayOfWeek") == null)
                throw new RuntimeException("Day of week is required.");
            schedule.setDayOfWeek(Schedule.DayOfWeek.valueOf(body.get("dayOfWeek")));
            if (body.get("timeStart") == null || body.get("timeEnd") == null)
                throw new RuntimeException("Start time and end time are required.");
            schedule.setTimeStart(java.time.LocalTime.parse(body.get("timeStart")));
            schedule.setTimeEnd(java.time.LocalTime.parse(body.get("timeEnd")));
            return ResponseEntity.ok(scheduleService.update(schedule, id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid day of week value."));
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid time format. Use HH:mm (e.g. 08:00)."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    // LAYER 1 → LAYER 2: Triggered by app.js doDeleteSchedule() when the registrar confirms schedule deletion
    // LAYER 2 → LAYER 3: Delegates to scheduleService.delete() which calls deleteByScheduleId() on the repository
    // LAYER 2 → LAYER 1: Returns a simple success message JSON
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        scheduleService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Schedule deleted"));
    }
}

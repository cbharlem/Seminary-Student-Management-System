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
import com.seminary.sms.service.AuditService;
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
    private final AuditService auditService;
    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final StudentService studentService;
    private final EmailService emailService;

    // LAYER 1 → LAYER 2: Triggered by app.js loadEnrollment() to populate the enrollment table
    // LAYER 2 → LAYER 3: Delegates to enrollmentService.getBySemester() if a semester filter is provided
    // LAYER 2 → LAYER 1: Returns a JSON list of Enrollment records matching the filter
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<Enrollment> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return enrollmentService.getBySemester(semester);
        return enrollmentRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js viewStudent() to load enrollment history in the student detail panel
    // LAYER 2 → LAYER 3: Delegates to enrollmentService.getEnrollmentsByStudent()
    // LAYER 2 → LAYER 1: Returns a JSON list of all enrollments for this student across all semesters
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('Registrar','Admin') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Enrollment> getByStudent(@PathVariable String studentId) {
        return enrollmentService.getEnrollmentsByStudent(studentId);
    }

    // Returns all admitted applicants who have not yet been enrolled (no student record yet)
    @GetMapping("/admitted-applicants")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> getAdmittedApplicants() {
        List<Application> admitted = applicationRepository
            .findByApplicationStatus(Application.ApplicationStatus.Admitted);
        List<Map<String, Object>> result = admitted.stream()
            .filter(app -> !studentRepository.existsByApplication_ApplicationId(app.getApplicationId()))
            .map(app -> {
                Applicant a = app.getApplicant();
                return Map.<String, Object>of(
                    "applicantId", a.getApplicantId(),
                    "applicationId", app.getApplicationId(),
                    "name", a.getFirstName() + " " + a.getLastName(),
                    "email", a.getEmail() != null ? a.getEmail() : "",
                    "programId", a.getAppliedProgram() != null ? a.getAppliedProgram().getProgramId() : "",
                    "programName", a.getAppliedProgram() != null ? a.getAppliedProgram().getProgramName() : ""
                );
            }).toList();
        return ResponseEntity.ok(result);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveEnrollment() when the registrar enrolls a student.
    // Accepts either studentId (re-enrollment) or applicantId (first-time enrollment from admitted applicant).
    // First-time enrollment creates the student record + user account, sends credentials email, then enrolls.
    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> enroll(@RequestBody Map<String, String> body) {
        try {
            Program program = programRepository.findByProgramId(body.get("programId"))
                .orElseThrow(() -> new RuntimeException("Program not found"));
            Semester semester = semesterRepository.findBySemesterId(body.get("semesterId"))
                .orElseThrow(() -> new RuntimeException("Semester not found"));
            Section section = body.containsKey("sectionId") && body.get("sectionId") != null && !body.get("sectionId").isBlank()
                ? sectionRepository.findBySectionId(body.get("sectionId")).orElse(null) : null;
            // SECURITY (A07): Validate yearLevel bounds to prevent corrupt data
            int yearLevel = Integer.parseInt(body.getOrDefault("yearLevel", "1"));
            if (yearLevel < 1 || yearLevel > 10)
                throw new RuntimeException("Year level must be between 1 and 10.");

            // ── First-time enrollment from admitted applicant ──────────────────
            if (body.containsKey("applicantId") && body.get("applicantId") != null && !body.get("applicantId").isBlank()) {
                String applicantId = body.get("applicantId");
                Applicant applicant = applicantRepository.findByApplicantId(applicantId)
                    .orElseThrow(() -> new RuntimeException("Applicant not found"));
                Application application = applicationRepository.findByApplicant_ApplicantId(applicantId)
                    .orElseThrow(() -> new RuntimeException("Application not found"));

                if (application.getApplicationStatus() != Application.ApplicationStatus.Admitted) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Applicant must be in Admitted status before enrolling."));
                }
                if (studentRepository.existsByApplication_ApplicationId(application.getApplicationId())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "A student record already exists for this applicant."));
                }

                // Generate student ID
                String year = String.valueOf(java.time.LocalDate.now().getYear());
                String studentId = "S" + year + "-" + String.format("%03d", 1 + studentRepository.count());

                // Build student record from applicant data
                Student student = Student.builder()
                    .studentId(studentId)
                    .application(application)
                    .firstName(applicant.getFirstName())
                    .middleName(applicant.getMiddleName())
                    .lastName(applicant.getLastName())
                    .dateOfBirth(applicant.getDateOfBirth())
                    .email(applicant.getEmail())
                    .contactNumber(applicant.getContactNumber())
                    .address(applicant.getAddress())
                    .nationality(applicant.getNationality())
                    .religion(applicant.getReligion())
                    .fatherName(applicant.getFatherName())
                    .fatherOccupation(applicant.getFatherOccupation())
                    .motherName(applicant.getMotherName())
                    .motherOccupation(applicant.getMotherOccupation())
                    .guardianName(applicant.getGuardianName())
                    .guardianContact(applicant.getGuardianContact())
                    .seminaryLevel(applicant.getSeminaryLevel() != null
                        ? Student.SeminaryLevel.valueOf(applicant.getSeminaryLevel().name())
                        : Student.SeminaryLevel.College)
                    .currentYearLevel(yearLevel)
                    .program(program)
                    .currentStatus(Student.StudentStatus.Active)
                    .build();

                // Create student record + user account
                String tempPassword = studentService.createWithAccount(student, studentId);

                // Mark application as Enrolled
                application.setApplicationStatus(Application.ApplicationStatus.Enrolled);
                applicationRepository.save(application);

                // Send credentials email asynchronously (fire-and-forget — does not block response)
                boolean emailSent = applicant.getEmail() != null && !applicant.getEmail().isBlank();
                if (emailSent) {
                    emailService.sendCredentials(
                        applicant.getEmail(), studentId, tempPassword,
                        applicant.getFirstName() + " " + applicant.getLastName()
                    );
                }

                // Save enrollment
                Enrollment enrolled = enrollmentService.enroll(student, program, semester, section, yearLevel);
                auditService.log("CREATE", "Student", "First enrollment: created student " + studentId + " from applicant " + applicantId);
                auditService.log("CREATE", "Enrollment", "Enrolled student " + studentId + " in semester " + semester.getSemesterId());

                java.util.Map<String, Object> response = new java.util.HashMap<>();
                response.put("enrollment", enrolled);
                response.put("studentId", studentId);
                response.put("temporaryPassword", tempPassword);
                response.put("emailSent", emailSent);
                response.put("isFirstEnrollment", true);
                return ResponseEntity.ok(response);
            }

            // ── Re-enrollment of existing student ─────────────────────────────
            Student student = studentRepository.findByStudentId(body.get("studentId"))
                .orElseThrow(() -> new RuntimeException("Student not found"));
            Enrollment enrolled = enrollmentService.enroll(student, program, semester, section, yearLevel);
            auditService.log("CREATE", "Enrollment", "Enrolled student " + student.getStudentId() + " in semester " + semester.getSemesterId());
            return ResponseEntity.ok(Map.of("enrollment", enrolled, "isFirstEnrollment", false));
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
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
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
    private final AuditService auditService;

    // LAYER 1 → LAYER 2: Triggered by app.js loadGrades() to populate the grades management table
    // LAYER 2 → LAYER 3: Delegates to gradeService.getGradesBySemester() if filtered, otherwise returns all
    // LAYER 2 → LAYER 1: Returns a JSON list of Grade records
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<Grade> getAll(@RequestParam(required = false) String semester) {
        if (semester != null) return gradeService.getGradesBySemester(semester);
        return gradeRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js viewStudent() to show a student's grades in the detail panel
    // LAYER 2 → LAYER 3: Delegates to gradeService filtered by student and optionally by semester
    // LAYER 2 → LAYER 1: Returns a JSON list of Grade records for the specified student
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('Registrar','Admin') or @studentSecurity.isOwner(authentication, #studentId)")
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
            Grade saved = gradeService.saveGrade(grade, user);
            auditService.log("CREATE", "Grade", "Created grade record ID: " + saved.getGradeId());
            return ResponseEntity.ok(saved);
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
            Grade updated = gradeService.updateGrade(id, mtCS, mtExam, fnCS, fnExam, status, remarks, user);
            auditService.log("UPDATE", "Grade", "Updated grade record ID: " + id);
            return ResponseEntity.ok(updated);
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
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
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

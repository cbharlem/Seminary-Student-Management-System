package com.seminary.sms.controller;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import com.seminary.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.PageRequest;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

// ── Students ──────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
class StudentController {

    private final StudentRepository studentRepository;
    private final StudentService studentService;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> getAll(@RequestParam(required = false) String q,
                                     @RequestParam(required = false) String program,
                                     @RequestParam(required = false) String status) {
        try {
            if (q != null && !q.isBlank()) return ResponseEntity.ok(studentService.searchStudents(q));
            if (status != null) {
                // SECURITY (A08): Enum poisoning — validate status before valueOf to return 400 not 500
                Student.StudentStatus st = Student.StudentStatus.valueOf(status);
                if (program != null)
                    return ResponseEntity.ok(studentRepository.findByCurrentStatusAndProgram_ProgramId(st, program));
                return ResponseEntity.ok(studentRepository.findByCurrentStatus(st));
            }
            if (program != null) return ResponseEntity.ok(studentRepository.findByProgram_ProgramId(program));
            // SECURITY (A05): Cap unfiltered results to prevent large query DoS
            return ResponseEntity.ok(studentRepository.findAll(PageRequest.of(0, 500)).getContent());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value."));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #id)")
    public ResponseEntity<Student> getById(@PathVariable String id) {
        return studentRepository.findByStudentId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Student> create(@RequestBody Student student) {
        String year = String.valueOf(java.time.LocalDate.now().getYear());
        student.setStudentId("S" + year + "-" + String.format("%03d", 1 + studentRepository.count()));
        resolveStudentRefs(student);
        return ResponseEntity.ok(studentService.save(student));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Student> update(@PathVariable String id, @RequestBody Student student) {
        if (!studentRepository.existsByStudentId(id)) return ResponseEntity.notFound().build();
        student.setStudentId(id);
        resolveStudentRefs(student);
        return ResponseEntity.ok(studentService.save(student));
    }

    private void resolveStudentRefs(Student student) {
        if (student.getProgram() != null && student.getProgram().getProgramId() != null)
            programRepository.findByProgramId(student.getProgram().getProgramId()).ifPresent(student::setProgram);
        if (student.getApplication() != null && student.getApplication().getApplicationId() != null)
            applicationRepository.findByApplicationId(student.getApplication().getApplicationId()).ifPresent(student::setApplication);
        if (student.getUser() != null && student.getUser().getUserId() != null)
            userRepository.findByUserId(student.getUser().getUserId()).ifPresent(student::setUser);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> updateStatus(@PathVariable String id, @RequestParam String status) {
        try {
            // SECURITY (A08): Enum poisoning — catch invalid status values
            studentService.updateStatus(id, Student.StudentStatus.valueOf(status));
            return ResponseEntity.ok(Map.of("message", "Status updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value."));
        }
    }
}

// ── Applicants ────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/applicants")
@RequiredArgsConstructor
class ApplicantController {

    private final ApplicantRepository applicantRepository;
    private final ApplicantService applicantService;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final StudentRepository studentRepository;
    private final StudentService studentService;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Applicant> getAll() {
        // SECURITY (A05): Cap unfiltered results to prevent large query DoS
        List<Applicant> applicants = applicantRepository.findAll(PageRequest.of(0, 500)).getContent();
        // Build a map of applicantId -> applicationStatus in one query
        Map<String, String> statusMap = new HashMap<>();
        applicationRepository.findAll().forEach(app -> {
            if (app.getApplicant() != null) {
                statusMap.put(app.getApplicant().getApplicantId(), app.getApplicationStatus().name());
            }
        });
        applicants.forEach(a -> a.setApplicationStatus(statusMap.get(a.getApplicantId())));
        return applicants;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Applicant> getById(@PathVariable String id) {
        return applicantRepository.findByApplicantId(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Applicant> create(@RequestBody Applicant applicant) {
        applicant.setApplicantId("P-" + String.format("%04d", 1001 + applicantRepository.count()));
        resolveApplicantRefs(applicant);
        Applicant saved = applicantService.save(applicant);

        // Auto-create a matching Application record
        SchoolYear activeYear = schoolYearRepository.findByIsActiveTrue().orElse(null);
        if (activeYear == null) activeYear = schoolYearRepository.findAll().stream().findFirst().orElse(null);
        if (activeYear != null) {
            Application app = Application.builder()
                .applicationId("APP-" + String.format("%04d", 1001 + applicationRepository.count()))
                .applicant(saved)
                .applicationDate(java.time.LocalDate.now())
                .schoolYear(activeYear)
                .applicationStatus(Application.ApplicationStatus.Applied)
                .build();
            applicationRepository.save(app);
        }

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Applicant> update(@PathVariable String id, @RequestBody Applicant body) {
        Applicant existing = applicantRepository.findByApplicantId(id)
            .orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        // Update fields on the managed entity (preserves the integer PK / index)
        existing.setFirstName(body.getFirstName());
        existing.setLastName(body.getLastName());
        existing.setMiddleName(body.getMiddleName());
        existing.setDateOfBirth(body.getDateOfBirth());
        existing.setEmail(body.getEmail());
        existing.setContactNumber(body.getContactNumber());
        existing.setSeminaryLevel(body.getSeminaryLevel());
        existing.setAddress(body.getAddress());
        existing.setLastSchoolAttended(body.getLastSchoolAttended());
        existing.setLastSchoolYear(body.getLastSchoolYear());
        existing.setLastYearLevel(body.getLastYearLevel());
        existing.setFatherName(body.getFatherName());
        existing.setFatherOccupation(body.getFatherOccupation());
        existing.setMotherName(body.getMotherName());
        existing.setMotherOccupation(body.getMotherOccupation());
        existing.setGuardianName(body.getGuardianName());
        existing.setGuardianContact(body.getGuardianContact());
        existing.setNationality(body.getNationality());
        existing.setReligion(body.getReligion());
        if (body.getAppliedProgram() != null && body.getAppliedProgram().getProgramId() != null)
            programRepository.findByProgramId(body.getAppliedProgram().getProgramId()).ifPresent(existing::setAppliedProgram);
        return ResponseEntity.ok(applicantService.save(existing));
    }

    private void resolveApplicantRefs(Applicant applicant) {
        if (applicant.getAppliedProgram() != null && applicant.getAppliedProgram().getProgramId() != null)
            programRepository.findByProgramId(applicant.getAppliedProgram().getProgramId()).ifPresent(applicant::setAppliedProgram);
    }

    @GetMapping("/{id}/application")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Application> getApplication(@PathVariable String id) {
        return applicantService.getApplicationByApplicant(id).map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/applications/{appId}/status")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> updateAppStatus(@PathVariable String appId, @RequestParam String status) {
        try {
            // SECURITY (A08): Enum poisoning — catch invalid status values
            return ResponseEntity.ok(applicantService.updateStatus(appId, Application.ApplicationStatus.valueOf(status)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value."));
        }
    }

    @GetMapping("/{id}/exams")
    @PreAuthorize("hasRole('Registrar')")
    public List<EntranceExam> getExams(@PathVariable String id) {
        return applicantService.getExamsByApplicant(id);
    }

    @PostMapping("/{id}/exams")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> recordExam(@PathVariable String id, @RequestBody EntranceExam exam) {
        // SECURITY (A08): Removed ID from exception message to prevent information disclosure
        Applicant applicant = applicantRepository.findByApplicantId(id)
            .orElseThrow(() -> new RuntimeException("Applicant not found"));
        exam.setApplicant(applicant);
        exam.setExamId("EXM-" + System.currentTimeMillis());
        return ResponseEntity.ok(applicantService.recordExam(exam));
    }

    @PostMapping("/{id}/admit")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> admit(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            // 1. Find applicant
            // SECURITY (A08): Removed ID from exception message to prevent information disclosure
            Applicant applicant = applicantRepository.findByApplicantId(id)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));

            // 2. Find application — create one if missing (for applicants added before auto-create was implemented)
            Application application = applicationRepository.findByApplicant_ApplicantId(id)
                .orElseGet(() -> {
                    SchoolYear activeYear2 = schoolYearRepository.findByIsActiveTrue()
                        .orElseGet(() -> schoolYearRepository.findAll().stream().findFirst()
                            .orElseThrow(() -> new RuntimeException("No school year found")));
                    Application newApp = Application.builder()
                        .applicationId("APP-" + String.format("%04d", 1001 + applicationRepository.count()))
                        .applicant(applicant)
                        .applicationDate(java.time.LocalDate.now())
                        .schoolYear(activeYear2)
                        .applicationStatus(Application.ApplicationStatus.Confirmed)
                        .build();
                    return applicationRepository.save(newApp);
                });

            // 3. Prevent duplicate — one applicant can only become one student
            if (studentRepository.existsByApplication_ApplicationId(application.getApplicationId())) {
                return ResponseEntity.badRequest().body(Map.of("error", "A student record already exists for this applicant."));
            }

            // 4. Resolve program — use applicant's applied program; only allow override if it matches
            String appliedProgramId = applicant.getAppliedProgram() != null
                ? applicant.getAppliedProgram().getProgramId() : null;
            String requestedProgramId = body.get("programId") != null ? body.get("programId").toString() : null;

            // SECURITY (A01): Reject if the caller is trying to assign a different program than applied
            if (requestedProgramId != null && appliedProgramId != null
                    && !requestedProgramId.equals(appliedProgramId)) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Cannot admit applicant to a program different from their applied program."));
            }

            // SECURITY (A01): Require at least one program — prevent null program assignment
            String programId = requestedProgramId != null ? requestedProgramId : appliedProgramId;
            if (programId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "A program must be specified for admission."));
            }

            // SECURITY (A08): Removed program ID from exception message
            com.seminary.sms.entity.Program program = programRepository.findByProgramId(programId)
                .orElseThrow(() -> new RuntimeException("Program not found"));

            // 5. Year level — SECURITY (A07): Validate bounds to prevent corrupt data
            int yearLevel = 1;
            if (body.get("yearLevel") != null) {
                try {
                    yearLevel = Integer.parseInt(body.get("yearLevel").toString());
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Year level must be a number."));
                }
            }
            if (yearLevel < 1 || yearLevel > 10) {
                return ResponseEntity.badRequest().body(Map.of("error", "Year level must be between 1 and 10."));
            }

            // 6. Generate student ID
            String year = String.valueOf(java.time.LocalDate.now().getYear());
            String studentId = "S" + year + "-" + String.format("%03d", 1 + studentRepository.count());

            // 7. Build student from applicant data
            com.seminary.sms.entity.Student student = com.seminary.sms.entity.Student.builder()
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
                    ? com.seminary.sms.entity.Student.SeminaryLevel.valueOf(applicant.getSeminaryLevel().name())
                    : com.seminary.sms.entity.Student.SeminaryLevel.College)
                .currentYearLevel(yearLevel)
                .program(program)
                .currentStatus(com.seminary.sms.entity.Student.StudentStatus.Active)
                .build();

            // 8. Create student record + login account with a random temporary password
            // SECURITY (A07): Temp password is returned once to the registrar to hand to the student
            String tempPassword = studentService.createWithAccount(student, studentId);

            // 9. Mark application as Admitted
            application.setApplicationStatus(Application.ApplicationStatus.Admitted);
            applicationRepository.save(application);

            return ResponseEntity.ok(Map.of(
                "message", "Student admitted successfully",
                "studentId", studentId,
                "temporaryPassword", tempPassword,
                "note", "Give this temporary password to the student. They should change it on first login."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }
}

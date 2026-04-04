package com.seminary.sms.controller;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import com.seminary.sms.service.AlumniService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// ── Curriculum ────────────────────────────────────────────────────────────────
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/curriculum")
@RequiredArgsConstructor
class CurriculumController {

    private final CourseRepository courseRepository;
    private final ProgramRepository programRepository;
    private final PrerequisiteRepository prerequisiteRepository;

    @GetMapping("/programs")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Program> getPrograms() {
        return programRepository.findByIsActiveTrue();
    }

    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Course> getCourses(@RequestParam(required = false) String program) {
        if (program != null) return courseRepository.findByProgram_ProgramIdAndIsActiveTrue(program);
        return courseRepository.findByIsActiveTrue();
    }

    @GetMapping("/courses/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Prerequisite> getPrerequisites(@PathVariable String id) {
        return prerequisiteRepository.findByCourse_CourseId(id);
    }

    @PostMapping("/courses")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Course> addCourse(@RequestBody Course course) {
        course.setCourseId("CRS" + String.format("%03d", 1 + courseRepository.count()));
        if (course.getProgram() != null && course.getProgram().getProgramId() != null)
            programRepository.findByProgramId(course.getProgram().getProgramId()).ifPresent(course::setProgram);
        return ResponseEntity.ok(courseRepository.save(course));
    }

    @PutMapping("/courses/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Course> updateCourse(@PathVariable String id, @RequestBody Course course) {
        Course existing = courseRepository.findByCourseId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (course.getCourseCode()     != null) existing.setCourseCode(course.getCourseCode());
        if (course.getCourseName()     != null) existing.setCourseName(course.getCourseName());
        if (course.getUnits()          != null) existing.setUnits(course.getUnits());
        if (course.getYearLevel()      != null) existing.setYearLevel(course.getYearLevel());
        if (course.getSemesterNumber() != null) existing.setSemesterNumber(course.getSemesterNumber());
        if (course.getIsActive()       != null) existing.setIsActive(course.getIsActive());
        if (course.getProgram() != null && course.getProgram().getProgramId() != null)
            programRepository.findByProgramId(course.getProgram().getProgramId()).ifPresent(existing::setProgram);
        return ResponseEntity.ok(courseRepository.save(existing));
    }

    @DeleteMapping("/courses/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Void> deleteCourse(@PathVariable String id) {
        Course existing = courseRepository.findByCourseId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        prerequisiteRepository.deleteByCourse_CourseId(id);
        prerequisiteRepository.deleteByPrerequisiteCourse_CourseId(id);
        courseRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/prerequisites")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Prerequisite> addPrerequisite(@RequestBody Prerequisite prereq) {
        prereq.setPrerequisiteId("PRE-" + System.currentTimeMillis());
        if (prereq.getCourse() != null && prereq.getCourse().getCourseId() != null)
            courseRepository.findByCourseId(prereq.getCourse().getCourseId()).ifPresent(prereq::setCourse);
        if (prereq.getPrerequisiteCourse() != null && prereq.getPrerequisiteCourse().getCourseId() != null)
            courseRepository.findByCourseId(prereq.getPrerequisiteCourse().getCourseId()).ifPresent(prereq::setPrerequisiteCourse);
        return ResponseEntity.ok(prerequisiteRepository.save(prereq));
    }

    @DeleteMapping("/prerequisites/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> deletePrerequisite(@PathVariable Integer id) {
        prerequisiteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Prerequisite removed"));
    }
}

// ── Sections, Instructors, Rooms ──────────────────────────────────────────────
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/sections")
@RequiredArgsConstructor
class SectionController {

    private final SectionRepository sectionRepository;
    private final InstructorRepository instructorRepository;
    private final RoomRepository roomRepository;
    private final ProgramRepository programRepository;
    private final SemesterRepository semesterRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Section> getSections(@RequestParam(required = false) String semester) {
        if (semester != null) return sectionRepository.findBySemester_SemesterId(semester)
            .stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).toList();
        return sectionRepository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Section> create(@RequestBody Section section) {
        section.setSectionId("SEC-" + String.format("%04d", 1001 + sectionRepository.count()));
        if (section.getProgram() != null && section.getProgram().getProgramId() != null)
            programRepository.findByProgramId(section.getProgram().getProgramId()).ifPresent(section::setProgram);
        if (section.getSemester() != null && section.getSemester().getSemesterId() != null)
            semesterRepository.findBySemesterId(section.getSemester().getSemesterId()).ifPresent(section::setSemester);
        return ResponseEntity.ok(sectionRepository.save(section));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Section> update(@PathVariable String id, @RequestBody Section section) {
        Section existing = sectionRepository.findBySectionId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setSectionCode(section.getSectionCode());
        existing.setSectionName(section.getSectionName());
        existing.setYearLevel(section.getYearLevel());
        existing.setCapacity(section.getCapacity());
        if (section.getProgram() != null && section.getProgram().getProgramId() != null)
            programRepository.findByProgramId(section.getProgram().getProgramId()).ifPresent(existing::setProgram);
        if (section.getSemester() != null && section.getSemester().getSemesterId() != null)
            semesterRepository.findBySemesterId(section.getSemester().getSemesterId()).ifPresent(existing::setSemester);
        return ResponseEntity.ok(sectionRepository.save(existing));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Section existing = sectionRepository.findBySectionId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setIsActive(false);
        sectionRepository.save(existing);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/instructors")
    @PreAuthorize("hasRole('Registrar')")
    public List<Instructor> getInstructors() {
        return instructorRepository.findByIsActiveTrue();
    }

    @PostMapping("/instructors")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Instructor> addInstructor(@RequestBody Instructor instructor) {
        instructor.setInstructorId("INS-" + String.format("%03d", 1 + instructorRepository.count()));
        return ResponseEntity.ok(instructorRepository.save(instructor));
    }

    @PutMapping("/instructors/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Instructor> updateInstructor(@PathVariable String id,
                                                        @RequestBody Instructor instructor) {
        if (!instructorRepository.existsByInstructorId(id)) return ResponseEntity.notFound().build();
        instructor.setInstructorId(id);
        return ResponseEntity.ok(instructorRepository.save(instructor));
    }

    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Room> getRooms() {
        return roomRepository.findByIsActiveTrue();
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Room> addRoom(@RequestBody Room room) {
        room.setRoomId("RM-" + String.format("%03d", 1 + roomRepository.count()));
        return ResponseEntity.ok(roomRepository.save(room));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Room> updateRoom(@PathVariable String id, @RequestBody Room room) {
        if (!roomRepository.existsByRoomId(id)) return ResponseEntity.notFound().build();
        room.setRoomId(id);
        return ResponseEntity.ok(roomRepository.save(room));
    }
}

// ── Alumni ────────────────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/alumni")
@RequiredArgsConstructor
class AlumniController {

    private final AlumniRepository alumniRepository;
    private final AlumniService alumniService;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<Alumni> getAll() {
        return alumniService.getAll();
    }

    @PostMapping("/graduate/{studentId}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> graduate(@PathVariable String studentId,
                                       @RequestBody Map<String, String> body) {
        try {
            LocalDate gradDate = LocalDate.parse(body.get("graduationDate"));
            String honors = body.get("honors");
            return ResponseEntity.ok(alumniService.graduateStudent(studentId, gradDate, honors));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Alumni> update(@PathVariable String id, @RequestBody Alumni alumni) {
        if (!alumniRepository.existsByAlumniId(id)) return ResponseEntity.notFound().build();
        alumni.setAlumniId(id);
        return ResponseEntity.ok(alumniService.update(alumni));
    }
}

// ── User Management ────────────────────────────────────────────────────────────
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        if (userRepository.existsByUsername(body.get("username"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }
        try {
            User user = User.builder()
                .userId("USR-" + String.format("%04d", 1001 + userRepository.count()))
                .username(body.get("username"))
                .passwordHash(passwordEncoder.encode(body.get("password")))
                // SECURITY (A08): Enum poisoning — wrap valueOf to return 400 on invalid role
                .role(User.Role.valueOf(body.get("role")))
                .isActive(true)
                .build();
            return ResponseEntity.ok(userRepository.save(user));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    @PatchMapping("/{userId}/toggle")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> toggle(@PathVariable String userId) {
        // SECURITY (A01): Use business key (userId) instead of sequential integer to prevent IDOR
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }

    // SECURITY (A07): Registrars generate a random temp password — they cannot choose one.
    // This prevents registrars from knowing a student's actual password.
    @PatchMapping("/{userId}/generate-temp-password")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> generateTempPassword(@PathVariable String userId) {
        // SECURITY (A01): Use business key (userId) instead of sequential integer to prevent IDOR
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("temporaryPassword", tempPassword,
            "message", "Temporary password generated. Give this to the user — it will not be shown again."));
    }

    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}

// ── School Years / Semesters ──────────────────────────────────────────────────
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/school-years")
@RequiredArgsConstructor
class SchoolYearController {

    private final SchoolYearRepository schoolYearRepository;
    private final SemesterRepository semesterRepository;

    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public List<SchoolYear> getAll() {
        return schoolYearRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<SchoolYear> create(@RequestBody SchoolYear sy) {
        return ResponseEntity.ok(schoolYearRepository.save(sy));
    }

    @GetMapping("/semesters")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public List<Semester> getSemesters() {
        return semesterRepository.findAll();
    }

    @GetMapping("/semesters/active")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public ResponseEntity<Semester> getActiveSemester() {
        return semesterRepository.findByIsActiveTrue()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/semesters/{id}/activate")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Semester> activateSemester(@PathVariable String id) {
        // Deactivate all first
        semesterRepository.findAll().forEach(s -> {
            s.setIsActive(false);
            semesterRepository.save(s);
        });
        Semester sem = semesterRepository.findBySemesterId(id)
            .orElseThrow(() -> new RuntimeException("Semester not found: " + id));
        sem.setIsActive(true);
        return ResponseEntity.ok(semesterRepository.save(sem));
    }

    @PostMapping("/semesters")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Semester> createSemester(@RequestBody Semester semester) {
        return ResponseEntity.ok(semesterRepository.save(semester));
    }
}

// ── Public (no auth) ──────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
class PublicController {

    private final SemesterRepository semesterRepository;

    /** Returns the active semester label for the login page badge. No auth required. */
    @GetMapping("/active-semester")
    public ResponseEntity<Map<String, String>> getActiveSemester() {
        return semesterRepository.findActiveWithSchoolYear()
            .map(sem -> {
                String label = sem.getSemesterLabel() + " \u2022 " + sem.getSchoolYear().getYearLabel();
                return ResponseEntity.ok(Map.of("label", label));
            })
            .orElse(ResponseEntity.ok(Map.of("label", "")));
    }
}

// ── Documents ─────────────────────────────────────────────────────────────────
@SuppressWarnings("null")
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
class DocumentController {

    private final DocumentRepository documentRepository;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasRole('Registrar') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Document> getByStudent(@PathVariable String studentId) {
        return documentRepository.findByStudent_StudentId(studentId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }
}

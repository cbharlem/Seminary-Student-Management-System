package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (AdminControllers.java)
// This file contains several controllers bundled together, each handling
// a different group of admin-only API endpoints:
//
//   CurriculumController   → @RequestMapping("/api/curriculum")
//      Manages programs, courses, and prerequisite rules.
//      Talks directly to: CourseRepository, ProgramRepository, PrerequisiteRepository
//
//   SectionController      → @RequestMapping("/api/sections")
//      Manages sections, instructors, and rooms.
//      Talks directly to: SectionRepository, InstructorRepository, RoomRepository,
//                         ProgramRepository, SemesterRepository
//
//   AlumniController       → @RequestMapping("/api/alumni")
//      Manages alumni records (graduate a student, update, or unmark as alumni).
//      Delegates to: AlumniService (for business logic), AlumniRepository (for existence checks)
//
//   UserController         → @RequestMapping("/api/users")
//      Manages user accounts (create, toggle active/inactive, generate temp password).
//      Talks directly to: UserRepository
//
//   SchoolYearController   → @RequestMapping("/api/school-years")
//      Manages school years and semesters (create, list, activate a semester).
//      Talks directly to: SchoolYearRepository, SemesterRepository
//
//   PublicController       → @RequestMapping("/api/public")
//      No authentication required. Provides the active semester label for the login page.
//      Talks directly to: SemesterRepository
//
//   DocumentController     → @RequestMapping("/api/documents")
//      Lists and deletes student document records.
//      Talks directly to: DocumentRepository
//
// LAYER 2 → LAYER 3: AlumniController delegates to AlumniService for complex logic.
// LAYER 2 → LAYER 4: Most controllers here talk directly to repositories for simple CRUD.
// LAYER 2 → LAYER 1: All endpoints return JSON that the frontend (api.js / app.js) consumes.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import com.seminary.sms.service.AlumniService;
import com.seminary.sms.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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

    // LAYER 1 → LAYER 2: Triggered by app.js when dropdowns or the curriculum page need the list of active programs
    // LAYER 2 → LAYER 4: Calls programRepository.findByIsActiveTrue() — no service needed here
    // LAYER 2 → LAYER 1: Returns a JSON list of active Program objects
    @GetMapping("/programs")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Program> getPrograms() {
        return programRepository.findByIsActiveTrue();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadCurriculum() to fill the curriculum table
    // LAYER 2 → LAYER 4: Calls courseRepository filtered by program and active status
    // LAYER 2 → LAYER 1: Returns a JSON list of Course objects matching the filter
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Course> getCourses(@RequestParam(required = false) String program) {
        if (program != null) return courseRepository.findByProgram_ProgramIdAndIsActiveTrue(program);
        return courseRepository.findByIsActiveTrue();
    }

    // LAYER 1 → LAYER 2: Called when displaying a course's prerequisite rules in the curriculum table
    // LAYER 2 → LAYER 4: Calls prerequisiteRepository.findByCourse_CourseId()
    // LAYER 2 → LAYER 1: Returns a JSON list of Prerequisite rules for the given course
    @GetMapping("/courses/{id}/prerequisites")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Prerequisite> getPrerequisites(@PathVariable String id) {
        return prerequisiteRepository.findByCourse_CourseId(id);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveCourse() when a new course is added in the curriculum
    // LAYER 2 → LAYER 4: Assigns a courseId, resolves the program reference, then calls courseRepository.save()
    // LAYER 2 → LAYER 1: Returns the saved Course JSON
    @PostMapping("/courses")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Course> addCourse(@RequestBody Course course) {
        course.setCourseId("CRS" + String.format("%03d", 1 + courseRepository.count()));
        if (course.getProgram() != null && course.getProgram().getProgramId() != null)
            programRepository.findByProgramId(course.getProgram().getProgramId()).ifPresent(course::setProgram);
        return ResponseEntity.ok(courseRepository.save(course));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveCourse() when editing an existing course
    // LAYER 2 → LAYER 4: Fetches the existing course, updates only non-null fields, then saves
    // LAYER 2 → LAYER 1: Returns the updated Course JSON, or 404 if not found
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

    // LAYER 1 → LAYER 2: Triggered by app.js confirmDeleteCourse() when the registrar confirms deletion
    // LAYER 2 → LAYER 4: Deletes all prerequisite rules for this course first, then deletes the course
    // LAYER 2 → LAYER 1: Returns HTTP 204 No Content on success, or 404 if not found
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

    // LAYER 1 → LAYER 2: Triggered when the registrar links one course as a prerequisite of another
    // LAYER 2 → LAYER 4: Resolves both course references from IDs, assigns a prerequisiteId, then saves
    // LAYER 2 → LAYER 1: Returns the saved Prerequisite JSON
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

    // LAYER 1 → LAYER 2: Triggered when the registrar removes a prerequisite rule from a course
    // LAYER 2 → LAYER 4: Calls prerequisiteRepository.deleteById() using the integer PK
    // LAYER 2 → LAYER 1: Returns a simple success message JSON
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

    // LAYER 1 → LAYER 2: Triggered by app.js loadSections() and dropdowns needing available sections
    // LAYER 2 → LAYER 4: Calls sectionRepository filtered by semester if provided; filters to active only
    // LAYER 2 → LAYER 1: Returns a JSON list of active Section objects
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Section> getSections(@RequestParam(required = false) String semester) {
        if (semester != null) return sectionRepository.findBySemester_SemesterId(semester)
            .stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).toList();
        return sectionRepository.findAll().stream().filter(s -> Boolean.TRUE.equals(s.getIsActive())).toList();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveSection() when adding a new section
    // LAYER 2 → LAYER 4: Assigns a sectionId, resolves program and semester references, then saves
    // LAYER 2 → LAYER 1: Returns the saved Section JSON
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

    // LAYER 1 → LAYER 2: Triggered by app.js saveSection() when editing an existing section
    // LAYER 2 → LAYER 4: Fetches the existing section, updates its fields, then saves
    // LAYER 2 → LAYER 1: Returns the updated Section JSON, or 404 if not found
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

    // LAYER 1 → LAYER 2: Triggered by app.js confirmDeleteSection() when a section is soft-deleted
    // LAYER 2 → LAYER 4: Sets isActive = false (soft delete — data is kept, just hidden from lists)
    // LAYER 2 → LAYER 1: Returns HTTP 204 No Content on success
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        Section existing = sectionRepository.findBySectionId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setIsActive(false);
        sectionRepository.save(existing);
        return ResponseEntity.noContent().build();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadInstructors() and schedule modal dropdowns
    // LAYER 2 → LAYER 4: Returns only active instructors from InstructorRepository
    // LAYER 2 → LAYER 1: Returns a JSON list of active Instructor objects
    @GetMapping("/instructors")
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<Instructor> getInstructors() {
        return instructorRepository.findByIsActiveTrue();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveInstructor() when the registrar adds a new instructor
    // LAYER 2 → LAYER 4: Assigns an instructorId, then calls instructorRepository.save()
    // LAYER 2 → LAYER 1: Returns the saved Instructor JSON
    @PostMapping("/instructors")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Instructor> addInstructor(@RequestBody Instructor instructor) {
        instructor.setInstructorId("INS-" + String.format("%03d", 1 + instructorRepository.count()));
        return ResponseEntity.ok(instructorRepository.save(instructor));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js when the registrar edits an existing instructor record
    // LAYER 2 → LAYER 4: Fetches existing record, patches editable fields, saves the updated record
    // LAYER 2 → LAYER 1: Returns the updated Instructor JSON, or 404 if not found
    @PutMapping("/instructors/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Instructor> updateInstructor(@PathVariable String id,
                                                        @RequestBody Instructor incoming) {
        return instructorRepository.findByInstructorId(id).map(existing -> {
            existing.setFirstName(incoming.getFirstName());
            existing.setMiddleName(incoming.getMiddleName());
            existing.setLastName(incoming.getLastName());
            existing.setEmail(incoming.getEmail());
            existing.setContactNumber(incoming.getContactNumber());
            existing.setSpecialization(incoming.getSpecialization());
            return ResponseEntity.ok(instructorRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadRooms() and schedule modal dropdowns
    // LAYER 2 → LAYER 4: Returns only active rooms from RoomRepository
    // LAYER 2 → LAYER 1: Returns a JSON list of active Room objects
    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Room> getRooms() {
        return roomRepository.findByIsActiveTrue();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveRoom() when a new room is added
    // LAYER 2 → LAYER 4: Assigns a roomId, then calls roomRepository.save()
    // LAYER 2 → LAYER 1: Returns the saved Room JSON
    @PostMapping("/rooms")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Room> addRoom(@RequestBody Room room) {
        room.setRoomId("RM-" + String.format("%03d", 1 + roomRepository.count()));
        return ResponseEntity.ok(roomRepository.save(room));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js when editing an existing room record
    // LAYER 2 → LAYER 4: Checks existence by roomId, then saves the updated record
    // LAYER 2 → LAYER 1: Returns the updated Room JSON, or 404 if not found
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
    private final AuditService auditService;

    // LAYER 1 → LAYER 2: Triggered by app.js loadAlumni() when the alumni page is opened
    // LAYER 2 → LAYER 3: Delegates to alumniService.getAll() which fetches from AlumniRepository
    // LAYER 2 → LAYER 1: Returns a JSON list of all Alumni records
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<Alumni> getAll() {
        return alumniService.getAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js graduateStudent() when the registrar graduates a student
    // LAYER 2 → LAYER 3: Delegates to alumniService.graduateStudent() which changes the student status and creates an Alumni record
    // LAYER 2 → LAYER 1: Returns the new Alumni JSON, or 400 if the student is already an alumnus
    @PostMapping("/graduate/{studentId}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> graduate(@PathVariable String studentId,
                                       @RequestBody Map<String, String> body) {
        try {
            LocalDate gradDate = LocalDate.parse(body.get("graduationDate"));
            String honors = body.get("honors");
            Alumni alum = alumniService.graduateStudent(studentId, gradDate, honors);
            auditService.log("CREATE", "Alumni", "Graduated student " + studentId + (honors != null && !honors.isBlank() ? " with honors: " + honors : ""));
            return ResponseEntity.ok(alum);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    // LAYER 1 → LAYER 2: Triggered by app.js when the registrar edits alumni details (ministry, address, notes)
    // LAYER 2 → LAYER 3: Delegates to alumniService.update() after verifying the alumni ID exists
    // LAYER 2 → LAYER 1: Returns the updated Alumni JSON, or 404 if not found
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Alumni> update(@PathVariable String id, @RequestBody Alumni alumni) {
        if (!alumniRepository.existsByAlumniId(id)) return ResponseEntity.notFound().build();
        alumni.setAlumniId(id);
        return ResponseEntity.ok(alumniService.update(alumni));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js confirmUnmarkAlumni() when reversing a graduation
    // LAYER 2 → LAYER 3: Delegates to alumniService.unmarkAlumni() which deletes the alumni record and reactivates the student
    // LAYER 2 → LAYER 1: Returns a success message JSON, or 400 if something fails
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> unmarkAlumni(@PathVariable String id) {
        try {
            alumniService.unmarkAlumni(id);
            return ResponseEntity.ok(Map.of("message", "Alumni record removed and student reactivated."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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
    private final AuditService auditService;

    // LAYER 1 → LAYER 2: Triggered by app.js loadUsers() when the registrar opens the Users page
    // LAYER 2 → LAYER 4: Calls userRepository.findAll() to retrieve all accounts
    // LAYER 2 → LAYER 1: Returns a JSON list of User objects (passwordHash is @JsonIgnore so it never appears)
    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public List<User> getAll() {
        return userRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveUser() when the registrar creates a new login account
    // LAYER 2 → LAYER 4: Validates uniqueness, hashes the password via BCrypt, then saves via userRepository
    // LAYER 2 → LAYER 1: Returns the saved User JSON, or 400 if the username already exists or role is invalid
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
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
            User saved = userRepository.save(user);
            auditService.log("CREATE", "User", "Created user account: " + saved.getUsername() + " (role: " + saved.getRole() + ")");
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid role specified."));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "An unexpected error occurred."));
        }
    }

    // LAYER 1 → LAYER 2: Triggered by app.js toggleUser() when enabling or disabling a user account
    // LAYER 2 → LAYER 4: Finds the user by userId, flips the isActive flag, then saves
    // LAYER 2 → LAYER 1: Returns a JSON with the new isActive value
    @PatchMapping("/{userId}/toggle")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> toggle(@PathVariable String userId) {
        // SECURITY (A01): Use business key (userId) instead of sequential integer to prevent IDOR
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setIsActive(!user.getIsActive());
        userRepository.save(user);
        auditService.log("UPDATE", "User", (user.getIsActive() ? "Enabled" : "Disabled") + " user account: " + user.getUsername());
        return ResponseEntity.ok(Map.of("isActive", user.getIsActive()));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js submitResetPw() when the registrar resets a user's password
    // LAYER 2 → LAYER 4: Generates a random password, hashes it, saves it, then returns the plain-text version once
    // LAYER 2 → LAYER 1: Returns the temporary password in the response — shown once and never stored in plain text
    // SECURITY (A07): Registrars generate a random temp password — they cannot choose one.
    // This prevents registrars from knowing a student's actual password.
    @PatchMapping("/{userId}/generate-temp-password")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> generateTempPassword(@PathVariable String userId) {
        // SECURITY (A01): Use business key (userId) instead of sequential integer to prevent IDOR
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        String tempPassword = generateTemporaryPassword();
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        userRepository.save(user);
        auditService.log("UPDATE", "User", "Reset password for user: " + user.getUsername());
        return ResponseEntity.ok(Map.of("temporaryPassword", tempPassword,
            "message", "Temporary password generated. Give this to the user — it will not be shown again."));
    }

    // Generates a 12-character random password using letters, numbers, and symbols.
    // Uses SecureRandom (cryptographically strong) so the output cannot be predicted.
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

    // LAYER 1 → LAYER 2: Triggered by app.js loadSchoolYears() when the registrar opens the School Years page
    // LAYER 2 → LAYER 4: Calls schoolYearRepository.findAll() to list all school years
    // LAYER 2 → LAYER 1: Returns a JSON list of SchoolYear objects
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<SchoolYear> getAll() {
        return schoolYearRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveSchoolYear() when creating a new school year record
    // LAYER 2 → LAYER 4: Calls schoolYearRepository.save() directly
    // LAYER 2 → LAYER 1: Returns the saved SchoolYear JSON
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<SchoolYear> create(@RequestBody SchoolYear sy) {
        return ResponseEntity.ok(schoolYearRepository.save(sy));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js during enrollment modal and semester dropdowns
    // LAYER 2 → LAYER 4: Calls semesterRepository.findAll() to list all semesters
    // LAYER 2 → LAYER 1: Returns a JSON list of all Semester objects
    @GetMapping("/semesters")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Semester> getSemesters() {
        return semesterRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js init() to load the current active semester label in the header
    // LAYER 2 → LAYER 4: Calls semesterRepository.findByIsActiveTrue() to find the one active semester
    // LAYER 2 → LAYER 1: Returns the active Semester JSON (200), or 404 if none is active
    @GetMapping("/semesters/active")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public ResponseEntity<Semester> getActiveSemester() {
        return semesterRepository.findByIsActiveTrue()
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    // LAYER 1 → LAYER 2: Triggered by app.js activateSem() when the registrar sets a new active semester
    // LAYER 2 → LAYER 4: Deactivates ALL semesters first, then activates the selected one
    // LAYER 2 → LAYER 1: Returns the newly activated Semester JSON
    @PatchMapping("/semesters/{id}/activate")
    @PreAuthorize("hasRole('Admin')")
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

    // LAYER 1 → LAYER 2: Triggered by app.js when the registrar adds a new semester record
    // LAYER 2 → LAYER 4: Calls semesterRepository.save() directly
    // LAYER 2 → LAYER 1: Returns the saved Semester JSON
    @PostMapping("/semesters")
    @PreAuthorize("hasRole('Admin')")
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

    // LAYER 1 → LAYER 2: Triggered by login.html JavaScript on page load to display the active semester badge
    // LAYER 2 → LAYER 4: Uses semesterRepository.findActiveWithSchoolYear() (JOIN FETCH query — one DB call)
    // LAYER 2 → LAYER 1: Returns a JSON object with a "label" string — no authentication required
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

    // LAYER 1 → LAYER 2: Triggered when the registrar views a student's document list
    // LAYER 2 → LAYER 4: Calls documentRepository.findByStudent_StudentId() — no service needed
    // LAYER 2 → LAYER 1: Returns a JSON list of Document records for the student
    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('Registrar','Admin') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Document> getByStudent(@PathVariable String studentId) {
        return documentRepository.findByStudent_StudentId(studentId);
    }

    // LAYER 1 → LAYER 2: Triggered when the registrar deletes a document record
    // LAYER 2 → LAYER 4: Calls documentRepository.deleteById() using the integer PK
    // LAYER 2 → LAYER 1: Returns a simple success message JSON
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }
}

// ── Audit Log (Admin only) ────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
class AdminAuditController {

    private final com.seminary.sms.repository.AuditLogRepository auditLogRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadAuditLog() when the Admin opens the Audit Logs page
    // LAYER 2 → LAYER 4: Calls auditLogRepository with pagination — newest entries first
    // LAYER 2 → LAYER 1: Returns a JSON map with items array and total count
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var result = auditLogRepository.findAllByOrderByTimestampDesc(PageRequest.of(page, Math.min(size, 100)));
        return ResponseEntity.ok(Map.of(
            "items", result.getContent(),
            "totalItems", result.getTotalElements(),
            "totalPages", result.getTotalPages(),
            "currentPage", page
        ));
    }
}

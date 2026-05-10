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
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
    private final CurriculumVersionRepository curriculumVersionRepository;
    private final EnrollmentSubjectRepository enrollmentSubjectRepository;
    private final GradeRepository gradeRepository;
    private final ScheduleRepository scheduleRepository;
    private final SemesterRepository semesterRepository;

    // LAYER 1 → LAYER 2: Called by app.js loadCurricula() to populate the version selector dropdown
    // LAYER 2 → LAYER 4: Fetches all curriculum versions for a program, newest first
    // LAYER 2 → LAYER 1: Returns a JSON list of Curriculum objects with isActive flag
    @GetMapping("/curricula")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Curriculum> getCurricula(@RequestParam(required = false) String program) {
        return program != null
            ? curriculumVersionRepository.findByProgram_ProgramIdOrderByCreatedAtDesc(program)
            : curriculumVersionRepository.findAll();
    }

    // LAYER 1 → LAYER 2: Called by app.js saveNewCurriculum() when registrar creates a new curriculum version
    // LAYER 2 → LAYER 4: Creates the new curriculum as an inactive draft. The registrar must manually
    //   activate it via PATCH /activate once all courses and prerequisites have been added.
    //   Optionally copies courses and prerequisites from a source curriculum.
    // LAYER 2 → LAYER 1: Returns the newly created Curriculum JSON
    @PostMapping("/curricula")
    @PreAuthorize("hasRole('Registrar')")
    @Transactional
    public ResponseEntity<?> createCurriculum(@RequestBody Map<String, String> body) {
        String programId  = body.get("programId");
        String label      = body.get("label");
        String copyFromId = body.get("copyFromCurriculumId");

        // Option A: block if enrollment is currently open
        boolean enrollmentOpen = semesterRepository.findByIsActiveTrue()
            .map(s -> Boolean.TRUE.equals(s.getEnrollmentOpen()))
            .orElse(false);
        if (enrollmentOpen) {
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot create a new curriculum while enrollment is open. Close enrollment first."));
        }

        Program program = programRepository.findByProgramId(programId).orElseThrow();

        // New curriculum starts as an inactive draft — registrar activates it manually when ready
        long seq = curriculumVersionRepository.count() + 1;
        Curriculum newCur = Curriculum.builder()
            .curriculumId("CUR-" + String.format("%03d", seq))
            .program(program)
            .label(label)
            .isActive(false)
            .build();
        curriculumVersionRepository.save(newCur);

        // Optionally copy all courses + prerequisite links from the source curriculum
        if (copyFromId != null && !copyFromId.isBlank()) {
            List<Course> sourceCourses = courseRepository.findByCurriculum_CurriculumId(copyFromId);
            Map<String, String> oldToNewId = new HashMap<>();

            for (Course src : sourceCourses) {
                long cseq = courseRepository.count() + 1;
                String newCourseId = "CRS" + String.format("%03d", cseq);
                Course copy = Course.builder()
                    .courseId(newCourseId)
                    .courseCode(src.getCourseCode())
                    .courseName(src.getCourseName())
                    .units(src.getUnits())
                    .program(program)
                    .curriculum(newCur)
                    .yearLevel(src.getYearLevel())
                    .semesterNumber(src.getSemesterNumber())
                    .isActive(true)
                    .build();
                courseRepository.save(copy);
                oldToNewId.put(src.getCourseId(), newCourseId);
            }

            // Re-create prerequisite links using the new course IDs
            long preSeq = prerequisiteRepository.count();
            for (Course src : sourceCourses) {
                List<Prerequisite> prereqs = prerequisiteRepository.findByCourse_CourseId(src.getCourseId());
                for (Prerequisite p : prereqs) {
                    String newCourseId = oldToNewId.get(src.getCourseId());
                    String newPrereqId = p.getPrerequisiteCourse() != null
                        ? oldToNewId.get(p.getPrerequisiteCourse().getCourseId()) : null;
                    if (newCourseId == null || newPrereqId == null) continue;
                    Prerequisite np = new Prerequisite();
                    np.setPrerequisiteId("PRE-" + String.format("%04d", ++preSeq));
                    courseRepository.findByCourseId(newCourseId).ifPresent(np::setCourse);
                    courseRepository.findByCourseId(newPrereqId).ifPresent(np::setPrerequisiteCourse);
                    prerequisiteRepository.save(np);
                }
            }
        }

        return ResponseEntity.ok(newCur);
    }

    @PatchMapping("/curricula/{id}/activate")
    @PreAuthorize("hasRole('Registrar')")
    @Transactional
    public ResponseEntity<?> activateCurriculum(@PathVariable String id) {
        Curriculum target = curriculumVersionRepository.findByCurriculumId(id).orElse(null);
        if (target == null) return ResponseEntity.notFound().build();

        // Option A: block if enrollment is currently open
        boolean enrollmentOpen = semesterRepository.findByIsActiveTrue()
            .map(s -> Boolean.TRUE.equals(s.getEnrollmentOpen()))
            .orElse(false);
        if (enrollmentOpen) {
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot switch the active curriculum while enrollment is open. Close enrollment first."));
        }

        // Option B: block if the target curriculum has no courses
        long targetCourseCount = courseRepository.findByCurriculum_CurriculumIdAndIsActiveTrue(id).size();
        if (targetCourseCount == 0) {
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot activate an empty curriculum. Add courses to it first."));
        }

        curriculumVersionRepository.findByProgram_ProgramIdAndIsActiveTrue(target.getProgram().getProgramId())
            .ifPresent(cur -> { cur.setIsActive(false); curriculumVersionRepository.save(cur); });
        target.setIsActive(true);
        return ResponseEntity.ok(curriculumVersionRepository.save(target));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js when dropdowns or the curriculum page need the list of active programs
    // LAYER 2 → LAYER 4: Calls programRepository.findByIsActiveTrue() — no service needed here
    // LAYER 2 → LAYER 1: Returns a JSON list of active Program objects
    @GetMapping("/programs")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Program> getPrograms() {
        return programRepository.findByIsActiveTrue();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadCurriculum() to fill the curriculum table
    // LAYER 2 → LAYER 4: Fetches courses, then attaches each course's prerequisite list so the
    //   frontend can display them without a second round-trip.
    // LAYER 2 → LAYER 1: Returns a JSON list of course maps, each containing a 'prerequisites' array
    @GetMapping("/courses")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Map<String, Object>> getCourses(
            @RequestParam(required = false) String program,
            @RequestParam(required = false) String curriculum) {
        List<Course> courses = curriculum != null
            ? courseRepository.findByCurriculum_CurriculumIdAndIsActiveTrue(curriculum)
            : program != null
                ? courseRepository.findByProgram_ProgramIdAndIsActiveTrue(program)
                : courseRepository.findByIsActiveTrue();
        return courses.stream().map(c -> {
            Map<String, Object> item = new java.util.LinkedHashMap<>();
            item.put("courseId",      c.getCourseId());
            item.put("courseCode",    c.getCourseCode());
            item.put("courseName",    c.getCourseName());
            item.put("units",         c.getUnits());
            item.put("yearLevel",     c.getYearLevel());
            item.put("semesterNumber",c.getSemesterNumber());
            item.put("isActive",      c.getIsActive());
            item.put("program",       c.getProgram());
            item.put("prerequisites", prerequisiteRepository.findByCourse_CourseId(c.getCourseId()));
            return item;
        }).toList();
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
    public ResponseEntity<?> addCourse(@RequestBody Course course) {
        course.setCourseId("CRS" + String.format("%03d", 1 + courseRepository.count()));
        if (course.getProgram() == null || course.getProgram().getProgramId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Program is required."));
        Program resolvedProgram = programRepository.findByProgramId(course.getProgram().getProgramId()).orElse(null);
        if (resolvedProgram == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Program not found."));
        course.setProgram(resolvedProgram);
        if (course.getCurriculum() != null && course.getCurriculum().getCurriculumId() != null)
            curriculumVersionRepository.findByCurriculumId(course.getCurriculum().getCurriculumId())
                .ifPresent(course::setCurriculum);
        return ResponseEntity.ok(courseRepository.save(course));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveCourse() when editing an existing course
    // LAYER 2 → LAYER 4: Fetches the existing course, updates only non-null fields, then saves
    // LAYER 2 → LAYER 1: Returns the updated Course JSON, or 404 if not found
    @PutMapping("/courses/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> updateCourse(@PathVariable String id, @RequestBody Course course) {
        Course existing = courseRepository.findByCourseId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        // Block year level or semester changes if the course is already referenced in
        // enrollments, grades, or schedules — changing those fields would corrupt existing records.
        boolean yearLevelChanging   = course.getYearLevel()      != null && !course.getYearLevel().equals(existing.getYearLevel());
        boolean semesterChanging    = course.getSemesterNumber()  != null && !course.getSemesterNumber().equals(existing.getSemesterNumber());
        if (yearLevelChanging || semesterChanging) {
            boolean hasEnrollments = enrollmentSubjectRepository.existsByCourse_CourseId(id);
            boolean hasGrades      = gradeRepository.existsByCourse_CourseId(id);
            boolean hasSchedules   = scheduleRepository.existsByCourse_CourseId(id);
            if (hasEnrollments || hasGrades || hasSchedules) {
                List<String> affected = new java.util.ArrayList<>();
                if (hasEnrollments) affected.add("student enrollments");
                if (hasGrades)      affected.add("grade records");
                if (hasSchedules)   affected.add("schedule entries");
                return ResponseEntity.status(409).body(Map.of("error",
                    "Year level and semester cannot be changed — this course has existing " +
                    String.join(", ", affected) + ". Create a new curriculum version to restructure courses."));
            }
        }

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
    @Transactional
    public ResponseEntity<?> deleteCourse(@PathVariable String id) {
        Course existing = courseRepository.findByCourseId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();

        boolean hasEnrollments = enrollmentSubjectRepository.existsByCourse_CourseId(id);
        boolean hasGrades      = gradeRepository.existsByCourse_CourseId(id);
        boolean hasSchedules   = scheduleRepository.existsByCourse_CourseId(id);
        if (hasEnrollments || hasGrades || hasSchedules) {
            List<String> affected = new java.util.ArrayList<>();
            if (hasEnrollments) affected.add("student enrollments");
            if (hasGrades)      affected.add("grade records");
            if (hasSchedules)   affected.add("schedule entries");
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot delete — this course has existing " +
                String.join(", ", affected) + ". Create a new curriculum version to remove courses."));
        }

        // Check if this course is listed as a prerequisite for other courses
        List<Prerequisite> usedAsPrereq = prerequisiteRepository.findByPrerequisiteCourse_CourseId(id);
        if (!usedAsPrereq.isEmpty()) {
            List<String> dependents = usedAsPrereq.stream()
                .map(p -> p.getCourse().getCourseCode())
                .distinct().sorted()
                .collect(java.util.stream.Collectors.toList());
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot delete — this course is a prerequisite for: " +
                String.join(", ", dependents) + ". Remove those prerequisite links first."));
        }

        prerequisiteRepository.deleteByCourse_CourseId(id);
        courseRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    // LAYER 1 → LAYER 2: Triggered when the registrar links one course as a prerequisite of another
    // LAYER 2 → LAYER 4: Resolves both course references from IDs, assigns a prerequisiteId, then saves
    // LAYER 2 → LAYER 1: Returns the saved Prerequisite JSON
    @PostMapping("/prerequisites")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> addPrerequisite(@RequestBody Prerequisite prereq) {
        prereq.setPrerequisiteId("PRE-" + System.currentTimeMillis());
        if (prereq.getCourse() == null || prereq.getCourse().getCourseId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Course is required."));
        Course resolvedCourse = courseRepository.findByCourseId(prereq.getCourse().getCourseId()).orElse(null);
        if (resolvedCourse == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Course not found."));
        prereq.setCourse(resolvedCourse);
        if (prereq.getPrerequisiteCourse() == null || prereq.getPrerequisiteCourse().getCourseId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Prerequisite course is required."));
        Course resolvedPrereq = courseRepository.findByCourseId(prereq.getPrerequisiteCourse().getCourseId()).orElse(null);
        if (resolvedPrereq == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Prerequisite course not found."));
        prereq.setPrerequisiteCourse(resolvedPrereq);
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
    private final ScheduleRepository scheduleRepository;
    private final StudentSectionRepository studentSectionRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadSections() and dropdowns needing available sections
    // LAYER 2 → LAYER 4: Calls sectionRepository filtered by semester if provided; filters to active only
    // LAYER 2 → LAYER 1: Returns a JSON list of active Section objects
    @GetMapping
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Section> getSections(@RequestParam(required = false) String semester) {
        if (semester != null) return sectionRepository.findBySemester_SemesterId(semester, Sort.by(Sort.Direction.DESC, "index"));
        return sectionRepository.findAll(Sort.by(Sort.Direction.DESC, "index"));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveSection() when adding a new section
    // LAYER 2 → LAYER 4: Assigns a sectionId, resolves program and semester references, then saves
    // LAYER 2 → LAYER 1: Returns the saved Section JSON
    @PostMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> create(@RequestBody Section section) {
        String semId = section.getSemester() != null ? section.getSemester().getSemesterId() : null;
        if (semId != null) {
            if (sectionRepository.existsBySectionCodeAndSemester_SemesterId(section.getSectionCode(), semId))
                return ResponseEntity.badRequest().body(Map.of("error", "A section with that code already exists in this semester"));
            if (sectionRepository.existsBySectionNameAndSemester_SemesterId(section.getSectionName(), semId))
                return ResponseEntity.badRequest().body(Map.of("error", "A section with that name already exists in this semester"));
        }
        long nextId = sectionRepository.findAll().stream()
                .map(s -> { try { return Long.parseLong(s.getSectionId().replace("SEC-", "")); } catch (Exception e) { return 0L; } })
                .max(Long::compareTo).orElse(1000L) + 1;
        section.setSectionId("SEC-" + String.format("%04d", nextId));
        if (section.getProgram() == null || section.getProgram().getProgramId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Program is required."));
        Program resolvedProg = programRepository.findByProgramId(section.getProgram().getProgramId()).orElse(null);
        if (resolvedProg == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Program not found."));
        section.setProgram(resolvedProg);
        if (section.getSemester() == null || section.getSemester().getSemesterId() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Semester is required."));
        Semester resolvedSem = semesterRepository.findBySemesterId(section.getSemester().getSemesterId()).orElse(null);
        if (resolvedSem == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Semester not found."));
        section.setSemester(resolvedSem);
        return ResponseEntity.ok(sectionRepository.save(section));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js saveSection() when editing an existing section
    // LAYER 2 → LAYER 4: Fetches the existing section, updates its fields, then saves
    // LAYER 2 → LAYER 1: Returns the updated Section JSON, or 404 if not found
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Section section) {
        Section existing = sectionRepository.findBySectionId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        String semId = section.getSemester() != null ? section.getSemester().getSemesterId()
                     : existing.getSemester() != null ? existing.getSemester().getSemesterId() : null;
        if (semId != null) {
            if (sectionRepository.existsBySectionCodeAndSemester_SemesterIdAndSectionIdNot(section.getSectionCode(), semId, id))
                return ResponseEntity.badRequest().body(Map.of("error", "A section with that code already exists in this semester"));
            if (sectionRepository.existsBySectionNameAndSemester_SemesterIdAndSectionIdNot(section.getSectionName(), semId, id))
                return ResponseEntity.badRequest().body(Map.of("error", "A section with that name already exists in this semester"));
        }
        if (section.getCapacity() != null) {
            long enrolled = studentSectionRepository.findBySection_SectionId(id).size();
            if (section.getCapacity() < enrolled)
                return ResponseEntity.badRequest().body(Map.of("error",
                    "Cannot set capacity to " + section.getCapacity() + " — section already has " + enrolled + " student(s) enrolled."));
        }
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

    // LAYER 1 → LAYER 2: Triggered by app.js confirmDeleteSection() when a section is deleted
    // LAYER 2 → LAYER 4: Hard-deletes the section after verifying no schedules reference it
    // LAYER 2 → LAYER 1: Returns HTTP 204 on success, or 409 if schedules exist
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        Section existing = sectionRepository.findBySectionId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (scheduleRepository.existsBySection_SectionId(id))
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot delete — this section has existing schedule entries. Remove the schedules first."));
        if (studentSectionRepository.existsBySection_SectionId(id))
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot delete — students are currently assigned to this section. Remove their section assignments first."));
        sectionRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js viewSectionStudents() to list enrolled students in a section
    // LAYER 2 → LAYER 4: Fetches all StudentSection records for the section, maps to a flat student summary
    // LAYER 2 → LAYER 1: Returns a JSON array of { studentId, fullName, currentYearLevel, program, dateAssigned }
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public ResponseEntity<?> getStudents(@PathVariable String id) {
        Section section = sectionRepository.findBySectionId(id).orElse(null);
        if (section == null) return ResponseEntity.notFound().build();
        List<StudentSection> assignments = studentSectionRepository.findBySection_SectionId(id);
        List<java.util.Map<String, Object>> result = assignments.stream().map(ss -> {
            Student st = ss.getStudent();
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("studentId",        st.getStudentId());
            m.put("fullName",         st.getFullName());
            m.put("currentYearLevel", st.getCurrentYearLevel());
            m.put("program",          st.getProgram() != null ? st.getProgram().getProgramCode() : "—");
            m.put("dateAssigned",     ss.getDateAssigned());
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(result);
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadInstructors() and schedule modal dropdowns
    // LAYER 2 → LAYER 4: Returns only active instructors from InstructorRepository
    // LAYER 2 → LAYER 1: Returns a JSON list of active Instructor objects
    @GetMapping("/instructors")
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public List<Instructor> getInstructors() {
        return instructorRepository.findByIsActiveTrue(Sort.by(Sort.Direction.DESC, "index"));
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

    // LAYER 1 → LAYER 2: Triggered by app.js confirmDeleteInstructor() when the registrar confirms deletion
    // LAYER 2 → LAYER 4: Blocked if instructor has active schedules — otherwise soft-deletes
    @DeleteMapping("/instructors/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> deleteInstructor(@PathVariable String id) {
        Instructor existing = instructorRepository.findByInstructorId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        List<Schedule> schedules = scheduleRepository.findByInstructor_InstructorId(id);
        if (!schedules.isEmpty()) {
            List<String> details = schedules.stream()
                .map(s -> s.getCourse() != null && s.getSection() != null
                    ? s.getCourse().getCourseCode() + " — " + s.getSection().getSectionName()
                    : s.getScheduleId())
                .toList();
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Cannot delete instructor — they have " + schedules.size() + " active schedule(s). Reassign them first.",
                "schedules", details
            ));
        }
        existing.setIsActive(false);
        instructorRepository.save(existing);
        return ResponseEntity.ok().<Void>build();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js loadRooms() and schedule modal dropdowns
    // LAYER 2 → LAYER 4: Returns only active rooms from RoomRepository
    // LAYER 2 → LAYER 1: Returns a JSON list of active Room objects
    @GetMapping("/rooms")
    @PreAuthorize("hasAnyRole('Registrar','Admin','Student')")
    public List<Room> getRooms() {
        return roomRepository.findByIsActiveTrue(Sort.by(Sort.Direction.DESC, "index"));
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
        return roomRepository.findByRoomId(id).map(existing -> {
            existing.setRoomName(room.getRoomName());
            existing.setBuilding(room.getBuilding());
            existing.setCapacity(room.getCapacity());
            return ResponseEntity.ok(roomRepository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }

    // LAYER 1 → LAYER 2: Triggered by app.js confirmDeleteRoom() when the registrar confirms deletion
    // LAYER 2 → LAYER 4: Blocks deletion if schedules reference this room, otherwise soft-deletes
    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> deleteRoom(@PathVariable String id) {
        Room existing = roomRepository.findByRoomId(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        if (scheduleRepository.existsByRoom_RoomId(id))
            return ResponseEntity.status(409).body(Map.of("error",
                "Cannot delete — this room has existing schedule entries. Remove the schedules first."));
        existing.setIsActive(false);
        roomRepository.save(existing);
        return ResponseEntity.noContent().build();
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

    // LAYER 1 → LAYER 2: Triggered by app.js openGraduateModal() before showing the graduation form
    // LAYER 2 → LAYER 3: Delegates to alumniService.checkGraduationEligibility() to get incomplete courses
    // LAYER 2 → LAYER 1: Returns { eligible: true } or { eligible: false, incomplete: ["Course A", ...] }
    @GetMapping("/eligibility/{studentId}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> checkEligibility(@PathVariable String studentId) {
        try {
            List<Map<String, Object>> incomplete = alumniService.checkGraduationEligibility(studentId);
            if (incomplete.isEmpty()) {
                return ResponseEntity.ok(Map.of("eligible", true));
            }
            return ResponseEntity.ok(Map.of("eligible", false, "incomplete", incomplete));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please fill in all required fields."));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // LAYER 1 → LAYER 2: Triggered by app.js graduateStudent() when the registrar graduates a student
    // LAYER 2 → LAYER 3: Delegates to alumniService.graduateStudent() which verifies curriculum completion, changes status, and creates an Alumni record
    // LAYER 2 → LAYER 1: Returns the new Alumni JSON, or 400 if requirements are not met or student is already an alumnus
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
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please fill in all required fields."));
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
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please fill in all required fields."));
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
        return userRepository.findAll(Sort.by(Sort.Direction.DESC, "index"));
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
        String roleStr = body.get("role");
        if (!"Registrar".equals(roleStr))
            return ResponseEntity.badRequest().body(Map.of("error", "Only Registrar accounts can be created here. Student accounts are created automatically on enrollment."));
        try {
            String email = body.get("email");
            User user = User.builder()
                .userId("USR-" + String.format("%04d", 1001 + userRepository.count()))
                .username(body.get("username"))
                .passwordHash(passwordEncoder.encode(body.get("password")))
                // SECURITY (A08): Enum poisoning — wrap valueOf to return 400 on invalid role
                .role(User.Role.valueOf(roleStr))
                .email(email != null && !email.isBlank() ? email.trim() : null)
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
    public ResponseEntity<?> toggle(@PathVariable String userId, Authentication auth) {
        // SECURITY (A01): Use business key (userId) instead of sequential integer to prevent IDOR
        User user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        if (user.getUsername().equals(auth.getName()))
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot disable your own account."));
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
        return semesterRepository.findAllByOrderBySchoolYear_IndexAscSemesterNumberAsc();
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

    // LAYER 1 → LAYER 2: Triggered by app.js saveSchoolYear() when creating a new semester
    // LAYER 2 → LAYER 4: Resolves the SchoolYear FK by schoolYearId, then saves the Semester
    // LAYER 2 → LAYER 1: Returns the saved Semester JSON
    @PostMapping("/semesters")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> createSemester(@RequestBody Map<String, Object> body) {
        String schoolYearId = (String) body.get("schoolYearId");
        SchoolYear sy = schoolYearRepository.findBySchoolYearId(schoolYearId)
            .orElseThrow(() -> new RuntimeException("School year not found: " + schoolYearId));
        Semester semester = new Semester();
        semester.setSemesterId((String) body.get("semesterId"));
        semester.setSchoolYear(sy);
        semester.setSemesterNumber(Integer.parseInt(body.get("semesterNumber").toString()));
        semester.setSemesterLabel((String) body.get("semesterLabel"));
        LocalDate startDate = LocalDate.parse((String) body.get("startDate"));
        LocalDate endDate   = LocalDate.parse((String) body.get("endDate"));
        if (!endDate.isAfter(startDate))
            return ResponseEntity.badRequest().body(Map.of("error", "End date must be after start date"));
        String[] syYears = sy.getYearLabel().split("-");
        int syStartYear = Integer.parseInt(syYears[0]);
        int syEndYear   = Integer.parseInt(syYears[1]);
        if (startDate.getYear() < syStartYear || startDate.getYear() > syEndYear)
            return ResponseEntity.badRequest().body(Map.of("error", "Start date must fall within the school year " + sy.getYearLabel()));
        if (endDate.getYear() < syStartYear || endDate.getYear() > syEndYear)
            return ResponseEntity.badRequest().body(Map.of("error", "End date must fall within the school year " + sy.getYearLabel()));
        if (ChronoUnit.DAYS.between(startDate, endDate) < 30)
            return ResponseEntity.badRequest().body(Map.of("error", "Semester must span at least 30 days"));
        boolean duplicate = semesterRepository.existsBySchoolYear_SchoolYearIdAndSemesterNumber(
            schoolYearId, Integer.parseInt(body.get("semesterNumber").toString()));
        if (duplicate)
            return ResponseEntity.badRequest().body(Map.of("error", "A semester with that number already exists for this school year"));
        semester.setStartDate(startDate);
        semester.setEndDate(endDate);
        semester.setIsActive(false);
        semester.setEnrollmentOpen(true);
        return ResponseEntity.ok(semesterRepository.save(semester));
    }

    @PutMapping("/semesters/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> updateSemester(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Semester sem = semesterRepository.findBySemesterId(id)
            .orElseThrow(() -> new RuntimeException("Semester not found: " + id));
        if (body.containsKey("semesterLabel"))  sem.setSemesterLabel((String) body.get("semesterLabel"));
        if (body.containsKey("semesterNumber")) sem.setSemesterNumber(Integer.parseInt(body.get("semesterNumber").toString()));
        LocalDate updStart = body.containsKey("startDate") ? LocalDate.parse((String) body.get("startDate")) : sem.getStartDate();
        LocalDate updEnd   = body.containsKey("endDate")   ? LocalDate.parse((String) body.get("endDate"))   : sem.getEndDate();
        if (!updEnd.isAfter(updStart))
            return ResponseEntity.badRequest().body(Map.of("error", "End date must be after start date"));
        String[] syYearsU = sem.getSchoolYear().getYearLabel().split("-");
        int syStartYearU = Integer.parseInt(syYearsU[0]);
        int syEndYearU   = Integer.parseInt(syYearsU[1]);
        if (updStart.getYear() < syStartYearU || updStart.getYear() > syEndYearU)
            return ResponseEntity.badRequest().body(Map.of("error", "Start date must fall within the school year " + sem.getSchoolYear().getYearLabel()));
        if (updEnd.getYear() < syStartYearU || updEnd.getYear() > syEndYearU)
            return ResponseEntity.badRequest().body(Map.of("error", "End date must fall within the school year " + sem.getSchoolYear().getYearLabel()));
        if (ChronoUnit.DAYS.between(updStart, updEnd) < 30)
            return ResponseEntity.badRequest().body(Map.of("error", "Semester must span at least 30 days"));
        sem.setStartDate(updStart);
        sem.setEndDate(updEnd);
        return ResponseEntity.ok(semesterRepository.save(sem));
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
    private final StudentRepository studentRepository;
    private final OnlineSubmissionRepository onlineSubmissionRepository;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('Registrar','Admin') or @studentSecurity.isOwner(authentication, #studentId)")
    public List<Document> getByStudent(@PathVariable String studentId) {
        return documentRepository.findByStudent_StudentId(studentId);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        documentRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Document deleted"));
    }

    @GetMapping("/{id}/file")
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public ResponseEntity<Resource> serveFile(@PathVariable Integer id) throws IOException {
        Document doc = documentRepository.findById(id).orElse(null);
        if (doc == null) return ResponseEntity.notFound().build();
        Path filePath = Paths.get("uploads", doc.getFilePath()).normalize().toAbsolutePath();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(filePath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + doc.getFileName() + "\"")
            .body(resource);
    }

    @PostMapping(value = "/student/{studentId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> upload(
            @PathVariable String studentId,
            @RequestParam String documentType,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String remarks) throws IOException {
        Student student = studentRepository.findByStudentId(studentId).orElse(null);
        if (student == null) return ResponseEntity.badRequest().body(Map.of("error", "Student not found"));
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String[] allowed = {"pdf", "jpg", "jpeg", "png"};
        boolean validExt = false;
        if (ext != null) for (String a : allowed) if (a.equalsIgnoreCase(ext)) { validExt = true; break; }
        if (!validExt) return ResponseEntity.badRequest().body(Map.of("error", "File must be PDF, JPG, or PNG"));
        String fileName = studentId + "-" + documentType.toLowerCase() + "-" + System.currentTimeMillis() + "." + ext.toLowerCase();
        Path dir = Paths.get("uploads", "documents", studentId);
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(fileName).toAbsolutePath());
        Document doc = Document.builder()
            .documentId("DOC-" + String.format("%04d", 1001 + documentRepository.count()))
            .student(student)
            .documentType(Document.DocumentType.valueOf(documentType))
            .fileName(fileName)
            .filePath("documents/" + studentId + "/" + fileName)
            .remarks(remarks != null && !remarks.isBlank() ? remarks.trim() : null)
            .build();
        return ResponseEntity.ok(documentRepository.save(doc));
    }

    @PostMapping("/backfill")
    @PreAuthorize("hasRole('Admin')")
    @Transactional
    public ResponseEntity<?> backfill() {
        record DocEntry(String path, Document.DocumentType type) {}
        List<Student> students = studentRepository.findAll();
        // Read count once before the loop so increments stay unique within the transaction
        long seq = documentRepository.count();
        int transferred = 0;
        int skipped = 0;
        for (Student student : students) {
            if (student.getEmail() == null || student.getEmail().isBlank()) { skipped++; continue; }
            java.util.Optional<com.seminary.sms.entity.OnlineSubmission> subOpt =
                onlineSubmissionRepository.findFirstByEmailAndStatus(
                    student.getEmail(), com.seminary.sms.entity.OnlineSubmission.SubmissionStatus.Accepted);
            if (subOpt.isEmpty()) { skipped++; continue; }
            com.seminary.sms.entity.OnlineSubmission sub = subOpt.get();
            java.util.List<DocEntry> entries = new java.util.ArrayList<>();
            if (sub.getBirthCertificate()        != null) entries.add(new DocEntry(sub.getBirthCertificate(),        Document.DocumentType.BirthCertificate));
            if (sub.getBaptismalCertificate()    != null) entries.add(new DocEntry(sub.getBaptismalCertificate(),    Document.DocumentType.BaptismalRecord));
            if (sub.getConfirmationCertificate() != null) entries.add(new DocEntry(sub.getConfirmationCertificate(), Document.DocumentType.ConfirmationRecord));
            if (sub.getReportCard()              != null) entries.add(new DocEntry(sub.getReportCard(),              Document.DocumentType.Form137));
            if (sub.getGoodMoral()               != null) entries.add(new DocEntry(sub.getGoodMoral(),               Document.DocumentType.GoodMoral));
            for (DocEntry entry : entries) {
                String filePath = "submissions/" + entry.path();
                if (documentRepository.existsByFilePath(filePath)) continue;
                String fname = entry.path().contains("/") ? entry.path().substring(entry.path().lastIndexOf('/') + 1) : entry.path();
                documentRepository.save(Document.builder()
                    .documentId("DOC-" + String.format("%04d", 1001 + ++seq))
                    .student(student)
                    .documentType(entry.type())
                    .fileName(fname)
                    .filePath(filePath)
                    .build());
                transferred++;
            }
        }
        return ResponseEntity.ok(Map.of(
            "message", "Backfill complete",
            "documentsTransferred", transferred,
            "studentsSkipped", skipped
        ));
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
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String type) {
        var pageable = PageRequest.of(page, Math.min(size, 100));
        com.seminary.sms.entity.AuditLog.LogType logType = null;
        try {
            if (type != null && !type.isBlank())
                logType = com.seminary.sms.entity.AuditLog.LogType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        var result = (logType != null)
            ? auditLogRepository.findByLogTypeOrderByTimestampDesc(logType, pageable)
            : auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        return ResponseEntity.ok(Map.of(
            "items", result.getContent(),
            "totalItems", result.getTotalElements(),
            "totalPages", result.getTotalPages(),
            "currentPage", page
        ));
    }
}

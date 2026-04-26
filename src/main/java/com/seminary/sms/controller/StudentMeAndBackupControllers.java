package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (StudentMeAndBackupControllers.java)
// This file bundles four controllers that serve the student-facing pages
// and the backup feature:
//
//   StudentMeController   → @RequestMapping("/api/students")
//      GET /api/students/me
//      Returns the currently logged-in student's own record.
//      Resolves identity via Spring Security → UserRepository → StudentRepository.
//      Accessible by both Registrar and Student roles.
//
//   GradeMeController     → @RequestMapping("/api/grades")
//      GET /api/grades/student/me
//      Returns the logged-in student's own grades.
//      Resolves identity the same way, then delegates to GradeService.
//
//   ScheduleMeController  → @RequestMapping("/api/schedule")
//      GET /api/schedule/mine
//      Returns the logged-in student's class schedule for the active semester.
//      Looks up the student's section assignment via StudentSectionRepository,
//      then fetches the schedule for that section via ScheduleService.
//
//   BackupController      → @RequestMapping("/api/backup")
//      GET  /api/backup/log    → returns all past backup log entries (Registrar only)
//      POST /api/backup/create → placeholder for a future database backup feature
//      Talks directly to: BackupLogRepository
//
// LAYER 2 → LAYER 3: GradeMeController delegates to GradeService;
//                     ScheduleMeController delegates to ScheduleService.
// LAYER 2 → LAYER 4: Calls UserRepository, StudentRepository, StudentSectionRepository,
//                     SemesterRepository, and BackupLogRepository directly.
// LAYER 2 → LAYER 1: Returns JSON responses consumed by student-facing pages in the frontend.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import com.seminary.sms.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// ── Student "Me" endpoints ───────────────────────────────────────────────────
// Used by the student-facing UI pages
@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
class StudentMeController {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadMyGrades() and loadMyProfile() to identify the logged-in student
    // LAYER 2 → LAYER 4: Resolves the student by chaining: Spring Security → UserRepository → StudentRepository
    // LAYER 2 → LAYER 1: Returns the Student JSON for the currently logged-in user, or 404 if no linked student record
    /**
     * GET /api/students/me
     * Returns the currently logged-in student's own record.
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public ResponseEntity<?> getMe(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
            .flatMap(u -> studentRepository.findByUser_UserId(u.getUserId()))
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}

// ── Grade /me endpoint ────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/grades")
@RequiredArgsConstructor
class GradeMeController {

    private final GradeService gradeService;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadMyGrades() to show the student their own grade card
    // LAYER 2 → LAYER 3: Resolves the student identity, then delegates to gradeService.getGradesByStudent()
    // LAYER 2 → LAYER 1: Returns a JSON list of Grade records for the logged-in student
    /**
     * GET /api/grades/student/me
     * Returns the currently logged-in student's grades.
     */
    @GetMapping("/student/me")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public ResponseEntity<?> getMyGrades(Authentication auth) {
        return userRepository.findByUsername(auth.getName())
            .flatMap(u -> studentRepository.findByUser_UserId(u.getUserId()))
            .map(s -> ResponseEntity.ok(gradeService.getGradesByStudent(s.getStudentId())))
            .orElse(ResponseEntity.notFound().build());
    }
}

// ── Schedule /mine endpoint ────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/schedule")
@RequiredArgsConstructor
class ScheduleMeController {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final StudentSectionRepository studentSectionRepository;
    private final ScheduleService scheduleService;
    private final SemesterRepository semesterRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadMySchedule() to show the student their class timetable
    // LAYER 2 → LAYER 4: Resolves the student, finds the active semester, looks up their section assignment,
    //   then delegates to scheduleService.getBySection() to fetch the schedule for that section
    // LAYER 2 → LAYER 1: Returns a JSON list of Schedule objects, or an empty list if not yet assigned to a section
    /**
     * GET /api/schedule/mine
     * Returns the active-semester schedule for the logged-in student.
     */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('Registrar','Student')")
    public ResponseEntity<?> getMySchedule(Authentication auth) {
        var userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) return ResponseEntity.notFound().build();

        var studentOpt = studentRepository.findByUser_UserId(userOpt.get().getUserId());
        if (studentOpt.isEmpty()) return ResponseEntity.notFound().build();

        Student student = studentOpt.get();
        var semOpt = semesterRepository.findByIsActiveTrue();
        if (semOpt.isEmpty()) return ResponseEntity.ok(List.of());

        String semId = semOpt.get().getSemesterId();
        var ssOpt = studentSectionRepository.findByStudent_StudentIdAndSemester_SemesterId(
            student.getStudentId(), semId);

        if (ssOpt.isEmpty()) return ResponseEntity.ok(List.of());

        return ResponseEntity.ok(
            scheduleService.getBySection(ssOpt.get().getSection().getSectionId())
        );
    }
}

// ── Backup Controller ─────────────────────────────────────────────────────────
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
class BackupController {

    private final BackupLogRepository backupLogRepository;

    // LAYER 1 → LAYER 2: Triggered by app.js loadBackup() to show the backup history table
    // LAYER 2 → LAYER 4: Calls backupLogRepository.findAllByOrderByBackupDateDesc() — newest entries first
    // LAYER 2 → LAYER 1: Returns a JSON list of BackupLog records
    /**
     * GET /api/backup/log
     * Returns the full list of past backup log entries.
     */
    @GetMapping("/log")
    @PreAuthorize("hasRole('Admin')")
    public List<BackupLog> getLog() {
        return backupLogRepository.findAllByOrderByBackupDateDesc();
    }

    // LAYER 1 → LAYER 2: Triggered by app.js triggerBackup() when the admin requests a backup
    // Currently returns a placeholder message — full implementation would use ProcessBuilder + mysqldump
    // LAYER 2 → LAYER 1: Returns an informational message JSON
    /**
     * POST /api/backup/create
     * NOTE: Full mysqldump integration requires runtime shell access.
     * For a local academic deployment, this endpoint signals the intent.
     * Implement with ProcessBuilder + mysqldump for production use.
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<?> createBackup(Authentication auth) {
        // SECURITY (A05): Never expose DB credentials, names, or implementation details in responses
        return ResponseEntity.ok(Map.of("message", "Backup feature is not yet implemented."));
    }
}

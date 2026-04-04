package com.seminary.sms.controller;

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

    /**
     * GET /api/backup/log
     * Returns the full list of past backup log entries.
     */
    @GetMapping("/log")
    @PreAuthorize("hasRole('Registrar')")
    public List<BackupLog> getLog() {
        return backupLogRepository.findAllByOrderByBackupDateDesc();
    }

    /**
     * POST /api/backup/create
     * NOTE: Full mysqldump integration requires runtime shell access.
     * For a local academic deployment, this endpoint signals the intent.
     * Implement with ProcessBuilder + mysqldump for production use.
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> createBackup(Authentication auth) {
        // SECURITY (A05): Never expose DB credentials, names, or implementation details in responses
        return ResponseEntity.ok(Map.of("message", "Backup feature is not yet implemented."));
    }
}

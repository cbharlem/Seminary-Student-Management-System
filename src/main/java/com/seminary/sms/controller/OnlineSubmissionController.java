package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (OnlineSubmission — two controllers in one file)
//
// PublicSubmissionController  (/api/public — NO auth required)
//   GET  /api/public/programs        → returns active programs for the apply.html dropdown
//   POST /api/public/apply           → receives a student's online admission form submission
//   GET  /api/public/check-status    → lets students look up their submission status by SUB-ID + email
//
// RegistrarSubmissionsController  (/api/submissions — Registrar only)
//   GET  /api/submissions            → list all submissions (optional ?status= filter)
//   GET  /api/submissions/{id}       → view a single submission's full details
//   POST /api/submissions/{id}/accept → accept: creates Applicant + Application, marks Accepted
//   POST /api/submissions/{id}/reject → reject: records a reason, marks Rejected
//
// LAYER 1 → LAYER 2: apply.html sends POST /api/public/apply on form submit.
//   app.js sends requests to /api/submissions for the registrar's Submissions screen.
// LAYER 2 → LAYER 4: Both controllers query repositories directly (no separate service needed).
// LAYER 2 → LAYER 1: ResponseEntity wraps results; Spring auto-converts to JSON.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// ── Public (no auth) ──────────────────────────────────────────────────────────
@RestController
@RequiredArgsConstructor
class PublicSubmissionController {

    private final ProgramRepository programRepository;
    private final OnlineSubmissionRepository submissionRepository;

    /**
     * Returns the list of active programs for the apply.html program dropdown.
     * Only exposes programId and programName — nothing sensitive.
     * No authentication required.
     */
    @GetMapping("/api/public/programs")
    public ResponseEntity<List<Map<String, String>>> getPrograms() {
        List<Map<String, String>> programs = programRepository.findByIsActiveTrue().stream()
            .map(p -> Map.of("programId", p.getProgramId(), "programName", p.getProgramName()))
            .toList();
        return ResponseEntity.ok(programs);
    }

    /**
     * Receives a student's online admission form submission from apply.html.
     * No authentication required — this is the public entry point.
     *
     * Validations performed:
     *  - Required fields: firstName, lastName, dateOfBirth, email, seminaryLevel, appliedProgram.programId
     *  - Email format (basic)
     *  - Duplicate check: reject if this email already has a Pending submission
     *
     * On success: saves a new OnlineSubmission with status=Pending and returns the submissionId.
     * SECURITY (A03): programId is resolved from the database — prevents sending arbitrary FK values.
     */
    @PostMapping("/api/public/apply")
    public ResponseEntity<?> submitApplication(@RequestBody OnlineSubmission submission) {
        // ── Required field validation ─────────────────────────────────────────
        if (isBlank(submission.getFirstName()))
            return ResponseEntity.badRequest().body(Map.of("error", "First name is required."));
        if (isBlank(submission.getLastName()))
            return ResponseEntity.badRequest().body(Map.of("error", "Last name is required."));
        if (submission.getDateOfBirth() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Date of birth is required."));
        if (isBlank(submission.getEmail()))
            return ResponseEntity.badRequest().body(Map.of("error", "Email is required."));
        if (!submission.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))
            return ResponseEntity.badRequest().body(Map.of("error", "Please enter a valid email address."));
        if (submission.getSeminaryLevel() == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Seminary level is required."));
        if (submission.getAppliedProgram() == null || isBlank(submission.getAppliedProgram().getProgramId()))
            return ResponseEntity.badRequest().body(Map.of("error", "Applied program is required."));

        // ── Duplicate pending check ───────────────────────────────────────────
        // SECURITY (A03): Prevent the same email from flooding the queue
        if (submissionRepository.existsByEmailAndStatus(
                submission.getEmail().trim(), OnlineSubmission.SubmissionStatus.Pending)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "An application with this email address is already pending review. " +
                          "Please wait for the registrar to process your existing submission."));
        }

        // ── Resolve program reference ─────────────────────────────────────────
        // SECURITY (A03): Always look up the program from the database — never trust the full object from input
        Program program = programRepository.findByProgramId(
                submission.getAppliedProgram().getProgramId()).orElse(null);
        if (program == null)
            return ResponseEntity.badRequest().body(Map.of("error", "Selected program not found."));

        // ── Sanitize and build the submission ─────────────────────────────────
        submission.setEmail(submission.getEmail().trim().toLowerCase());
        submission.setFirstName(submission.getFirstName().trim());
        submission.setLastName(submission.getLastName().trim());
        if (submission.getMiddleName() != null) submission.setMiddleName(submission.getMiddleName().trim());
        submission.setAppliedProgram(program);
        submission.setStatus(OnlineSubmission.SubmissionStatus.Pending);

        // Auto-generate SUB-#### id
        Integer maxNum = submissionRepository.findMaxSubmissionIdNumber();
        int nextNum = (maxNum == null) ? 1001 : (maxNum + 1);
        submission.setSubmissionId("SUB-" + String.format("%04d", nextNum));

        OnlineSubmission saved = submissionRepository.save(submission);
        return ResponseEntity.ok(Map.of("submissionId", saved.getSubmissionId()));
    }

    /**
     * Lets a student look up the status of their own submission.
     * Requires BOTH the submissionId AND the email used when applying —
     * this prevents anyone with just a SUB-ID from guessing another person's result.
     * No authentication required.
     *
     * Returns:
     *  - 200 with { status, firstName, lastName, programName, submittedAt, rejectionReason }
     *  - 404 if no submission matches the given ID + email combination
     * SECURITY (A01): email acts as a second factor — ID alone is not enough to see the result.
     */
    @GetMapping("/api/public/check-status")
    public ResponseEntity<?> checkStatus(
            @RequestParam String submissionId,
            @RequestParam String email) {

        if (isBlank(submissionId) || isBlank(email))
            return ResponseEntity.badRequest().body(Map.of("error", "Submission ID and email are required."));

        OnlineSubmission sub = submissionRepository
            .findBySubmissionId(submissionId.trim()).orElse(null);

        // SECURITY (A01): verify email matches — don't reveal whether the SUB-ID exists without a valid email
        if (sub == null || !sub.getEmail().equalsIgnoreCase(email.trim()))
            return ResponseEntity.status(404).body(Map.of(
                "error", "No submission found for the provided ID and email. Please check your details."));

        String programName = sub.getAppliedProgram() != null
            ? sub.getAppliedProgram().getProgramName() : "—";

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("status",          sub.getStatus().name());
        result.put("submissionId",    sub.getSubmissionId());
        result.put("firstName",       sub.getFirstName());
        result.put("lastName",        sub.getLastName());
        result.put("programName",     programName);
        result.put("seminaryLevel",   sub.getSeminaryLevel() != null ? sub.getSeminaryLevel().name() : "—");
        result.put("submittedAt",     sub.getSubmittedAt() != null ? sub.getSubmittedAt().toString() : null);
        result.put("reviewedAt",      sub.getReviewedAt() != null ? sub.getReviewedAt().toString() : null);
        result.put("rejectionReason", sub.getRejectionReason());
        return ResponseEntity.ok(result);
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}

// ── Registrar (auth required) ─────────────────────────────────────────────────
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
class RegistrarSubmissionsController {

    private final OnlineSubmissionRepository submissionRepository;
    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final SchoolYearRepository schoolYearRepository;
    private final ProgramRepository programRepository;

    /**
     * Returns all submissions, optionally filtered by status.
     * LAYER 1 → LAYER 2: Called by app.js loadSubmissions() on page load and tab switch.
     */
    @GetMapping
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<List<OnlineSubmission>> getAll(
            @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            try {
                // SECURITY (A08): Enum poisoning — validate status before valueOf to return 400 not 500
                OnlineSubmission.SubmissionStatus st = OnlineSubmission.SubmissionStatus.valueOf(status);
                return ResponseEntity.ok(submissionRepository.findByStatusOrderBySubmittedAtDesc(st));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }
        return ResponseEntity.ok(submissionRepository.findAllByOrderBySubmittedAtDesc());
    }

    /**
     * Returns a single submission's full details for the Review modal.
     * LAYER 1 → LAYER 2: Called by app.js openSubmissionDetail().
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<OnlineSubmission> getById(@PathVariable String id) {
        return submissionRepository.findBySubmissionId(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Accepts a pending submission: creates an official Applicant and Application record,
     * then marks the submission as Accepted.
     *
     * Accept flow:
     *  1. Load submission (404 if not found)
     *  2. Guard: only Pending submissions can be accepted (409 if already processed)
     *  3. Generate applicantId using same pattern as ApplicantController.create()
     *  4. Build Applicant from submission fields
     *  5. Save Applicant
     *  6. Build Application with status=Applied, linked to active SchoolYear
     *  7. Save Application
     *  8. Mark submission Accepted + set reviewedAt
     *  9. Return new applicantId to the frontend
     */
    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> accept(@PathVariable String id) {
        OnlineSubmission submission = submissionRepository.findBySubmissionId(id).orElse(null);
        if (submission == null)
            return ResponseEntity.notFound().build();
        if (submission.getStatus() != OnlineSubmission.SubmissionStatus.Pending)
            return ResponseEntity.status(409).body(Map.of("error", "This submission has already been processed."));

        // ── Build Applicant ───────────────────────────────────────────────────
        String applicantId = "P-" + String.format("%04d", 1001 + applicantRepository.count());
        Applicant applicant = Applicant.builder()
            .applicantId(applicantId)
            .firstName(submission.getFirstName())
            .middleName(submission.getMiddleName())
            .lastName(submission.getLastName())
            .dateOfBirth(submission.getDateOfBirth())
            .placeOfBirth(submission.getPlaceOfBirth())
            .gender(submission.getGender())
            .address(submission.getAddress())
            .contactNumber(submission.getContactNumber())
            .email(submission.getEmail())
            .nationality(submission.getNationality())
            .religion(submission.getReligion())
            .fatherName(submission.getFatherName())
            .fatherOccupation(submission.getFatherOccupation())
            .motherName(submission.getMotherName())
            .motherOccupation(submission.getMotherOccupation())
            .guardianName(submission.getGuardianName())
            .guardianContact(submission.getGuardianContact())
            .lastSchoolAttended(submission.getLastSchoolAttended())
            .lastSchoolYear(submission.getLastSchoolYear())
            .lastYearLevel(submission.getLastYearLevel())
            .seminaryLevel(submission.getSeminaryLevel())
            .appliedProgram(submission.getAppliedProgram())
            .build();
        Applicant saved = applicantRepository.save(applicant);

        // ── Build Application ─────────────────────────────────────────────────
        SchoolYear activeYear = schoolYearRepository.findByIsActiveTrue().orElse(null);
        if (activeYear == null)
            activeYear = schoolYearRepository.findAll().stream().findFirst().orElse(null);
        if (activeYear != null) {
            Application app = Application.builder()
                .applicationId("APP-" + String.format("%04d", 1001 + applicationRepository.count()))
                .applicant(saved)
                .applicationDate(LocalDate.now())
                .schoolYear(activeYear)
                .applicationStatus(Application.ApplicationStatus.Applied)
                .build();
            applicationRepository.save(app);
        }

        // ── Mark submission as Accepted ───────────────────────────────────────
        submission.setStatus(OnlineSubmission.SubmissionStatus.Accepted);
        submission.setReviewedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        return ResponseEntity.ok(Map.of("applicantId", applicantId));
    }

    /**
     * Rejects a pending submission with a reason provided by the registrar.
     * LAYER 1 → LAYER 2: Called by app.js rejectSubmission().
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<?> reject(@PathVariable String id,
                                    @RequestBody Map<String, String> body) {
        OnlineSubmission submission = submissionRepository.findBySubmissionId(id).orElse(null);
        if (submission == null)
            return ResponseEntity.notFound().build();
        if (submission.getStatus() != OnlineSubmission.SubmissionStatus.Pending)
            return ResponseEntity.status(409).body(Map.of("error", "This submission has already been processed."));

        String reason = body.getOrDefault("reason", "").trim();
        submission.setStatus(OnlineSubmission.SubmissionStatus.Rejected);
        submission.setRejectionReason(reason.isBlank() ? null : reason);
        submission.setReviewedAt(LocalDateTime.now());
        submissionRepository.save(submission);

        return ResponseEntity.ok(Map.of("message", "Submission rejected."));
    }
}

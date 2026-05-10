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

import com.seminary.sms.config.IpRateLimitService;
import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
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
    private final IpRateLimitService ipRateLimitService;

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
     * Receives a student's online admission form + required documents from apply.html.
     * No authentication required — this is the public entry point.
     * Accepts multipart/form-data so files can be uploaded alongside text fields.
     *
     * Required files: birthCertificate, baptismalCertificate, confirmationCertificate,
     *                 reportCard, goodMoral (PDF / JPG / PNG, max 5 MB each).
     * Files are saved to: uploads/submissions/{submissionId}/
     * SECURITY (A03): programId resolved from DB; path traversal prevented in filenames.
     */
    @PostMapping(value = "/api/public/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> submitApplication(
            HttpServletRequest request,
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam(required = false) String middleName,
            @RequestParam String dateOfBirth,
            @RequestParam(required = false) String placeOfBirth,
            @RequestParam(required = false) String gender,
            @RequestParam String email,
            @RequestParam(required = false) String contactNumber,
            @RequestParam(required = false) String nationality,
            @RequestParam(required = false) String religion,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String fatherName,
            @RequestParam(required = false) String fatherOccupation,
            @RequestParam(required = false) String motherName,
            @RequestParam(required = false) String motherOccupation,
            @RequestParam(required = false) String guardianName,
            @RequestParam(required = false) String guardianContact,
            @RequestParam(required = false) String lastSchoolAttended,
            @RequestParam(required = false) String lastSchoolYear,
            @RequestParam(required = false) String lastYearLevel,
            @RequestParam String seminaryLevel,
            @RequestParam String appliedProgram,
            @RequestParam MultipartFile birthCertificate,
            @RequestParam MultipartFile baptismalCertificate,
            @RequestParam MultipartFile confirmationCertificate,
            @RequestParam MultipartFile reportCard,
            @RequestParam MultipartFile goodMoral) {

        // SECURITY (A04): Rate limit by IP — max 3 submissions per hour per IP
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        if (!ipRateLimitService.isAllowed(ip))
            return ResponseEntity.status(429).body(Map.of("error",
                "Too many submissions from your network. Please try again later."));

        // ── Required field validation ─────────────────────────────────────────
        if (isBlank(firstName))      return bad("First name is required.");
        if (isBlank(lastName))       return bad("Last name is required.");
        if (isBlank(dateOfBirth))    return bad("Date of birth is required.");
        if (isBlank(email))          return bad("Email is required.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return bad("Please enter a valid email address.");
        if (isBlank(seminaryLevel))  return bad("Seminary level is required.");
        if (isBlank(appliedProgram)) return bad("Applied program is required.");

        // ── File validation ───────────────────────────────────────────────────
        if (isEmpty(birthCertificate))        return bad("Birth certificate is required.");
        if (isEmpty(baptismalCertificate))    return bad("Baptismal certificate is required.");
        if (isEmpty(confirmationCertificate)) return bad("Confirmation certificate is required.");
        if (isEmpty(reportCard))              return bad("Report card is required.");
        if (isEmpty(goodMoral))               return bad("Good moral certificate is required.");

        String[] allowed = {"pdf", "jpg", "jpeg", "png"};
        if (!validExt(birthCertificate, allowed))        return bad("Birth certificate must be PDF or image.");
        if (!validExt(baptismalCertificate, allowed))    return bad("Baptismal certificate must be PDF or image.");
        if (!validExt(confirmationCertificate, allowed)) return bad("Confirmation certificate must be PDF or image.");
        if (!validExt(reportCard, allowed))              return bad("Report card must be PDF or image.");
        if (!validExt(goodMoral, allowed))               return bad("Good moral certificate must be PDF or image.");

        // ── Duplicate pending check ───────────────────────────────────────────
        if (submissionRepository.existsByEmailAndStatus(
                email.trim().toLowerCase(), OnlineSubmission.SubmissionStatus.Pending))
            return bad("An application with this email is already pending review.");

        // ── Resolve program reference ─────────────────────────────────────────
        Program program = programRepository.findByProgramId(appliedProgram.trim()).orElse(null);
        if (program == null) return bad("Selected program not found.");

        // ── Generate submission ID ────────────────────────────────────────────
        Integer maxNum = submissionRepository.findMaxSubmissionIdNumber();
        String submissionId = "SUB-" + String.format("%04d", (maxNum == null) ? 1001 : (maxNum + 1));

        // ── Save uploaded files ───────────────────────────────────────────────
        try {
            Path dir = Paths.get("uploads", "submissions", submissionId);
            Files.createDirectories(dir);

            String bcPath    = saveFile(dir, submissionId, "birth-certificate",        birthCertificate);
            String bapPath   = saveFile(dir, submissionId, "baptismal-certificate",    baptismalCertificate);
            String confPath  = saveFile(dir, submissionId, "confirmation-certificate", confirmationCertificate);
            String rcPath    = saveFile(dir, submissionId, "report-card",              reportCard);
            String gmPath    = saveFile(dir, submissionId, "good-moral",               goodMoral);

            // ── Build and save the submission ─────────────────────────────────
            Applicant.SeminaryLevel level;
            try { level = Applicant.SeminaryLevel.valueOf(seminaryLevel); }
            catch (IllegalArgumentException e) { return bad("Invalid seminary level."); }

            OnlineSubmission submission = OnlineSubmission.builder()
                .submissionId(submissionId)
                .firstName(firstName.trim())
                .middleName(isBlank(middleName) ? null : middleName.trim())
                .lastName(lastName.trim())
                .dateOfBirth(LocalDate.parse(dateOfBirth))
                .placeOfBirth(isBlank(placeOfBirth) ? null : placeOfBirth.trim())
                .gender(isBlank(gender) ? Applicant.Gender.Male : Applicant.Gender.valueOf(gender))
                .email(email.trim().toLowerCase())
                .contactNumber(isBlank(contactNumber) ? null : contactNumber.trim())
                .nationality(isBlank(nationality) ? null : nationality.trim())
                .religion(isBlank(religion) ? null : religion.trim())
                .address(isBlank(address) ? null : address.trim())
                .fatherName(isBlank(fatherName) ? null : fatherName.trim())
                .fatherOccupation(isBlank(fatherOccupation) ? null : fatherOccupation.trim())
                .motherName(isBlank(motherName) ? null : motherName.trim())
                .motherOccupation(isBlank(motherOccupation) ? null : motherOccupation.trim())
                .guardianName(isBlank(guardianName) ? null : guardianName.trim())
                .guardianContact(isBlank(guardianContact) ? null : guardianContact.trim())
                .lastSchoolAttended(isBlank(lastSchoolAttended) ? null : lastSchoolAttended.trim())
                .lastSchoolYear(isBlank(lastSchoolYear) ? null : lastSchoolYear.trim())
                .lastYearLevel(isBlank(lastYearLevel) ? null : lastYearLevel.trim())
                .seminaryLevel(level)
                .appliedProgram(program)
                .birthCertificate(bcPath)
                .baptismalCertificate(bapPath)
                .confirmationCertificate(confPath)
                .reportCard(rcPath)
                .goodMoral(gmPath)
                .status(OnlineSubmission.SubmissionStatus.Pending)
                .build();

            OnlineSubmission saved = submissionRepository.save(submission);
            return ResponseEntity.ok(Map.of("submissionId", saved.getSubmissionId()));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Submission failed. Please check all fields and try again."));
        }
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.badRequest().body(Map.of("error", msg));
    }

    private boolean isEmpty(MultipartFile f) { return f == null || f.isEmpty(); }

    private boolean validExt(MultipartFile f, String[] allowed) {
        String ext = StringUtils.getFilenameExtension(f.getOriginalFilename());
        if (ext == null) return false;
        for (String a : allowed) if (a.equalsIgnoreCase(ext)) return true;
        return false;
    }

    private String saveFile(Path dir, String subId, String docType, MultipartFile file) throws IOException {
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String filename = subId + "-" + docType + "." + (ext != null ? ext.toLowerCase() : "bin");
        file.transferTo(dir.resolve(filename).toAbsolutePath());
        return subId + "/" + filename;
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
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
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
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
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
     * Serves an uploaded document file to the registrar for viewing/downloading.
     * SECURITY: path traversal prevented — dots and slashes in id/filename are rejected.
     */
    @GetMapping("/{id}/files/{filename:.+}")
    @PreAuthorize("hasAnyRole('Registrar','Admin')")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String id, @PathVariable String filename) throws IOException {
        if (id.contains("..") || id.contains("/") || filename.contains(".."))
            return ResponseEntity.badRequest().build();
        Path filePath = Paths.get("uploads", "submissions", id, filename).normalize().toAbsolutePath();
        Resource resource = new UrlResource(filePath.toUri());
        if (!resource.exists()) return ResponseEntity.notFound().build();
        String contentType = Files.probeContentType(filePath);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
            .body(resource);
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

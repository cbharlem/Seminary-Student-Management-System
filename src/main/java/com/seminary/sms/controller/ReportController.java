package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (ReportController)
// Handles PDF report generation for three official academic documents:
//   GET /api/reports/grade-card?studentId=&semesterId=   → Grade Card PDF
//   GET /api/reports/transcript?studentId=               → Transcript of Records PDF
//   GET /api/reports/ched-report?semesterId=             → CHED Compliance Report PDF
//
// All endpoints are restricted to Registrar role.
// Each endpoint validates required parameters, delegates to PdfReportService,
// and streams the resulting PDF bytes as a file download.
//
// LAYER 2 → LAYER 3: Calls PdfReportService to generate the PDF bytes.
// LAYER 2 → LAYER 1: Returns ResponseEntity<byte[]> with application/pdf headers.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.service.PdfReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('Registrar','Admin')")
public class ReportController {

    private final PdfReportService pdfReportService;

    // LAYER 1 → LAYER 2: Registrar clicks Generate on Grade Card report
    // LAYER 2 → LAYER 3: Calls PdfReportService.generateGradeCard()
    // LAYER 2 → LAYER 1: Returns PDF bytes as downloadable file
    @GetMapping("/grade-card")
    public ResponseEntity<byte[]> gradeCard(
            @RequestParam String studentId,
            @RequestParam String semesterId) {
        if (studentId == null || studentId.isBlank() || semesterId == null || semesterId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] pdf = pdfReportService.generateGradeCard(studentId, semesterId);
            return pdfResponse(pdf, "grade-card-" + studentId + ".pdf");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // LAYER 1 → LAYER 2: Registrar clicks Generate on Transcript of Records
    // LAYER 2 → LAYER 3: Calls PdfReportService.generateTranscript()
    // LAYER 2 → LAYER 1: Returns PDF bytes as downloadable file
    @GetMapping("/transcript")
    public ResponseEntity<byte[]> transcript(@RequestParam String studentId) {
        if (studentId == null || studentId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] pdf = pdfReportService.generateTranscript(studentId);
            return pdfResponse(pdf, "transcript-" + studentId + ".pdf");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    // LAYER 1 → LAYER 2: Registrar clicks Generate on CHED Report
    // LAYER 2 → LAYER 3: Calls PdfReportService.generateCHEDReport()
    // LAYER 2 → LAYER 1: Returns PDF bytes as downloadable file
    @GetMapping("/ched-report")
    public ResponseEntity<byte[]> chedReport(@RequestParam String semesterId) {
        if (semesterId == null || semesterId.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        try {
            byte[] pdf = pdfReportService.generateCHEDReport(semesterId);
            return pdfResponse(pdf, "ched-report-" + semesterId + ".pdf");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdf.length);
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}

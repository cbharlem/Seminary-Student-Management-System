package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (ApplicantService)
// Handles all business logic related to applicants, their applications,
// and entrance exam results.
//
// Repositories used:
//   ApplicantRepository    — to save and retrieve applicant personal records
//   ApplicationRepository  — to manage application records and status updates
//   EntranceExamRepository — to record and retrieve entrance exam results
//
// Business logic handled here:
//   - save()                      → saves a new or updated applicant record
//   - updateStatus()              → changes an application's status (e.g., from Applied to Admitted)
//   - recordExam()                → saves an entrance exam result for an applicant
//   - getApplicationsByStatus()   → filters applications by their current status
//   - getApplicationByApplicant() → finds the application linked to a given applicant
//   - getExamsByApplicant()       → retrieves all exam records for a given applicant
//
// LAYER 3 → LAYER 4: Calls the three repositories above to read/write the database.
// LAYER 4 → LAYER 3: Repositories return Applicant, Application, and EntranceExam objects.
// LAYER 3 → LAYER 2: StudentApplicantControllers calls this service and returns results to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final EntranceExamRepository entranceExamRepository;

    // LAYER 2 → LAYER 3: Called when the controller needs all applicants without filtering
    // LAYER 3 → LAYER 4: Calls applicantRepository.findAll()
    public List<Applicant> getAll() {
        return applicantRepository.findAll();
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.getById() to fetch one applicant's record
    // LAYER 3 → LAYER 4: Calls applicantRepository.findByApplicantId()
    public Optional<Applicant> getById(String id) {
        return applicantRepository.findByApplicantId(id);
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.create() and update() to persist the applicant
    // LAYER 3 → LAYER 4: Calls applicantRepository.save() — issues INSERT or UPDATE in tblApplicants
    @Transactional
    public Applicant save(Applicant applicant) {
        return applicantRepository.save(applicant);
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.updateAppStatus() when the registrar moves an application forward
    // LAYER 3 → LAYER 4: Finds the Application by ID, sets the new status, then saves
    @Transactional
    public Application updateStatus(String applicationId, Application.ApplicationStatus status) {
        Application app = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        app.setApplicationStatus(status);
        return applicationRepository.save(app);
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.recordExam() after the exam object is built and linked
    // LAYER 3 → LAYER 4: Calls entranceExamRepository.save() to persist the exam result
    @Transactional
    public EntranceExam recordExam(EntranceExam exam) {
        return entranceExamRepository.save(exam);
    }

    // LAYER 2 → LAYER 3: Utility method to filter applications by status (e.g. only Admitted ones)
    // LAYER 3 → LAYER 4: Calls applicationRepository.findByApplicationStatus()
    public List<Application> getApplicationsByStatus(Application.ApplicationStatus status) {
        return applicationRepository.findByApplicationStatus(status);
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController to load the application linked to a given applicant
    // LAYER 3 → LAYER 4: Calls applicationRepository.findByApplicant_ApplicantId()
    public Optional<Application> getApplicationByApplicant(String applicantId) {
        return applicationRepository.findByApplicant_ApplicantId(applicantId);
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.getExams() to load all exams for one applicant
    // LAYER 3 → LAYER 4: Calls entranceExamRepository.findByApplicant_ApplicantId()
    public List<EntranceExam> getExamsByApplicant(String applicantId) {
        return entranceExamRepository.findByApplicant_ApplicantId(applicantId);
    }
}

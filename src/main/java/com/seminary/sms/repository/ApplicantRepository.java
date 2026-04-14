package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (ApplicantRepository)
// Serves the Applicant entity — reads from and writes to the tblapplicants table.
//
// Spring auto-generates the SQL for each method based on the method name:
//   findByApplicantId   → SELECT * FROM tblapplicants WHERE fldApplicantID = ?
//   existsByApplicantId → returns true if any row has that applicant ID
//   findByEmail         → finds an applicant by their email address
//
// LAYER 4 → LAYER 5: Uses the Applicant entity class to map database rows to objects.
// LAYER 4 → LAYER 3: ApplicantService calls this repository to fetch and save applicants.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ApplicantRepository extends JpaRepository<Applicant, Integer> {

    // Auto-generates: SELECT * FROM tblapplicants WHERE fldApplicantID = ?
    // Called by: ApplicantService.getById() and ApplicantController for single-record lookups
    Optional<Applicant> findByApplicantId(String applicantId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblapplicants WHERE fldApplicantID = ?
    // Called by: ApplicantController to check existence before updates
    boolean existsByApplicantId(String applicantId);

    // Auto-generates: SELECT * FROM tblapplicants WHERE fldEmail = ?
    // Called by: duplicate-checking logic to prevent two applicants with the same email
    Optional<Applicant> findByEmail(String email);
}

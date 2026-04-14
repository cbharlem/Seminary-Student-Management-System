package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (EntranceExamRepository)
// Serves the EntranceExam entity — reads from and writes to the tblentranceexam table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByApplicant_Index       → all exam records for a given applicant (by PK)
//   findByApplicant_ApplicantId → all exam records for a given applicant (by business ID)
//
// LAYER 4 → LAYER 5: Uses the EntranceExam entity to map database rows to objects.
// LAYER 4 → LAYER 3: ApplicantService calls this to record and retrieve entrance exam results.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.EntranceExam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EntranceExamRepository extends JpaRepository<EntranceExam, Integer> {

    // Auto-generates: JOIN tblapplicants ON ... WHERE tblapplicants.fldIndex = ?
    // Called by: lookups that use the applicant's integer PK to retrieve their exam records
    List<EntranceExam> findByApplicant_Index(Integer applicantIndex);

    // Auto-generates: JOIN tblapplicants ON ... WHERE tblapplicants.fldApplicantID = ?
    // Called by: ApplicantService.getExams() to list all entrance exam results for an applicant
    List<EntranceExam> findByApplicant_ApplicantId(String applicantId);
}

package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (ApplicationRepository)
// Serves the Application entity — reads from and writes to the tblapplications table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByApplicationId             → finds one application by its business ID
//   findByApplicant_Index           → finds all applications for a given applicant (by PK)
//   findByApplicant_ApplicantId     → finds an application by the applicant's business ID
//   findByApplicationStatus         → filters applications by status (e.g., Admitted, Rejected)
//   findBySchoolYear_Index          → finds all applications for a given school year
//
// LAYER 4 → LAYER 5: Uses the Application entity to map rows to objects.
// LAYER 4 → LAYER 3: ApplicantService calls this repository to manage application records.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ApplicationRepository extends JpaRepository<Application, Integer> {

    // Auto-generates: SELECT * FROM tblapplications WHERE fldApplicationID = ?
    // Called by: ApplicantService.updateStatus() and ApplicantController.admit() to fetch a specific application
    Optional<Application> findByApplicationId(String applicationId);

    // Auto-generates: JOIN tblapplicants ON ... WHERE tblapplicants.fldIndex = ?
    // Called by: lookups that use the applicant's integer PK instead of their business ID
    List<Application> findByApplicant_Index(Integer applicantIndex);

    // Auto-generates: JOIN tblapplicants ON ... WHERE tblapplicants.fldApplicantID = ?
    // Called by: ApplicantService.getApplicationByApplicant() and ApplicantController.admit()
    Optional<Application> findByApplicant_ApplicantId(String applicantId);

    // Auto-generates: SELECT * FROM tblapplications WHERE fldApplicationStatus = ?
    // Called by: ApplicantService.getApplicationsByStatus() to filter by stage (e.g. Admitted)
    List<Application> findByApplicationStatus(Application.ApplicationStatus status);

    // Auto-generates: JOIN tblschoolyear ON ... WHERE tblschoolyear.fldIndex = ?
    // Called by: lookups scoped to a specific school year
    List<Application> findBySchoolYear_Index(Integer schoolYearIndex);
}

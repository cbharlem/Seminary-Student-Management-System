package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (ReportRepository)
// Serves the Report entity — reads from and writes to the tblreports table.
//
// Spring auto-generates SQL from the method name declared here:
//   findByStudent_Index → retrieves all report records generated for a specific student
//
// LAYER 4 → LAYER 5: Uses the Report entity to map database rows to objects.
// LAYER 4 → LAYER 2: Report-related controllers call this to log and retrieve reports.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReportRepository extends JpaRepository<Report, Integer> {

    // Auto-generates: JOIN tblstudents ON ... WHERE tblstudents.fldIndex = ?
    // Called by: report-related controllers to retrieve all report records for a specific student
    List<Report> findByStudent_Index(Integer studentIndex);
}

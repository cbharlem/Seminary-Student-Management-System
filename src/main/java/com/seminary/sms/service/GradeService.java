package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (GradeService)
// Handles all business logic for recording, updating, and computing student grades.
//
// Repositories used:
//   GradeRepository             — to save and retrieve grade records
//   EnrollmentSubjectRepository — to sync the subject's status when a grade is saved
//
// Business logic handled here:
//   - saveGrade()    → assigns the grade ID, records who entered it, calls computeFinalRating()
//                      on the Grade entity to calculate the average, then saves it.
//                      Also updates the linked EnrollmentSubject status to match
//                      (e.g., if grade is Passed, the subject status becomes Completed).
//   - updateGrade()  → finds an existing grade by its PK and calls saveGrade() to update it.
//   - computeGWA()   → calculates the student's General Weighted Average for a semester.
//                      Uses the formula: GWA = sum(finalRating × units) / sum(units).
//                      This is the Philippine 5-point scale where lower is better (1.0 = highest).
//
// LAYER 3 → LAYER 4: Calls GradeRepository and EnrollmentSubjectRepository.
// LAYER 4 → LAYER 3: Repositories return Grade and EnrollmentSubject objects.
// LAYER 3 → LAYER 2: GradeController calls this service and returns results to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class GradeService {

    private final GradeRepository gradeRepository;
    private final EnrollmentSubjectRepository enrollmentSubjectRepository;

    // LAYER 2 → LAYER 3: Called by GradeController and GradeMeController to fetch all grades for a student
    // LAYER 3 → LAYER 4: Calls gradeRepository.findByStudent_StudentId()
    public List<Grade> getGradesByStudent(String studentId) {
        return gradeRepository.findByStudent_StudentId(studentId);
    }

    // LAYER 2 → LAYER 3: Called by GradeController.getByStudent() when a semester filter is also provided
    // LAYER 3 → LAYER 4: Calls gradeRepository.findByStudent_StudentIdAndSemester_SemesterId()
    public List<Grade> getGradesByStudentAndSemester(String studentId, String semesterId) {
        return gradeRepository.findByStudent_StudentIdAndSemester_SemesterId(studentId, semesterId);
    }

    // LAYER 2 → LAYER 3: Called by GradeController.getAll() when a semester filter is provided
    // LAYER 3 → LAYER 4: Calls gradeRepository.findBySemester_SemesterId()
    public List<Grade> getGradesBySemester(String semesterId) {
        return gradeRepository.findBySemester_SemesterId(semesterId);
    }

    // LAYER 2 → LAYER 3: Utility method to compute a student's General Weighted Average for a semester
    // LAYER 3 → LAYER 4: Fetches all grades for the student and semester, then applies the weighted formula
    // Returns null if there are no graded subjects yet
    /**
     * Compute GWA using the 5-point scale (lower = better, passing ≤ 3.0)
     * GWA = sum(finalRating × units) / sum(units)
     */
    public BigDecimal computeGWA(String studentId, String semesterId) {
        List<Grade> grades = gradeRepository.findByStudent_StudentIdAndSemester_SemesterId(studentId, semesterId);
        if (grades.isEmpty()) return null;

        BigDecimal totalWeighted = BigDecimal.ZERO;
        int totalUnits = 0;

        for (Grade g : grades) {
            if (g.getFinalRating() != null && g.getCourse() != null) {
                int units = g.getCourse().getUnits();
                totalWeighted = totalWeighted.add(g.getFinalRating().multiply(new BigDecimal(units)));
                totalUnits += units;
            }
        }

        if (totalUnits == 0) return null;
        return totalWeighted.divide(new BigDecimal(totalUnits), 2, RoundingMode.HALF_UP);
    }

    // LAYER 2 → LAYER 3: Called by GradeController.save() and updateGrade() to persist a grade record
    // LAYER 3 → LAYER 4: Assigns ID, records who entered it, calls grade.computeFinalRating() to calculate
    //   the average, saves to gradeRepository, then also syncs the EnrollmentSubject status to match
    // @Transactional: both the grade save and the enrollment subject update succeed or both roll back
    @Transactional
    public Grade saveGrade(Grade grade, User enteredBy) {
        if (grade.getGradeId() == null) grade.setGradeId("GRD-" + System.currentTimeMillis());
        grade.setEnteredBy(enteredBy);
        grade.setDateEntered(LocalDateTime.now());
        grade.computeFinalRating();
        Grade saved = gradeRepository.save(grade);

        // Sync enrollment subject status with computed grade status
        if (saved.getEnrollmentSubject() != null) {
            EnrollmentSubject es = saved.getEnrollmentSubject();
            switch (saved.getGradeStatus()) {
                case Passed     -> es.setStatus(EnrollmentSubject.SubjectStatus.Completed);
                case Failed     -> es.setStatus(EnrollmentSubject.SubjectStatus.Failed);
                case Incomplete -> es.setStatus(EnrollmentSubject.SubjectStatus.Incomplete);
                case Dropped    -> es.setStatus(EnrollmentSubject.SubjectStatus.Dropped);
                default         -> {}
            }
            enrollmentSubjectRepository.save(es);
        }
        return saved;
    }

    // LAYER 2 → LAYER 3: Called by GradeController.update() when the registrar edits an existing grade
    // LAYER 3 → LAYER 4: Fetches the grade by its integer PK, updates all fields, then calls saveGrade()
    //   to recompute the final rating and sync the enrollment subject status
    @Transactional
    public Grade updateGrade(String gradeId,
                              BigDecimal midtermClassStanding, BigDecimal midtermExam,
                              BigDecimal finalClassStanding,   BigDecimal finalExam,
                              Grade.GradeStatus status, String remarks, User enteredBy) {
        Grade grade = gradeRepository.findByGradeId(gradeId)
            .orElseThrow(() -> new RuntimeException("Grade not found: " + gradeId));
        grade.setMidtermClassStanding(midtermClassStanding);
        grade.setMidtermExam(midtermExam);
        grade.setFinalClassStanding(finalClassStanding);
        grade.setFinalExam(finalExam);
        grade.setGradeStatus(status);
        grade.setRemarks(remarks);
        return saveGrade(grade, enteredBy);
    }
}

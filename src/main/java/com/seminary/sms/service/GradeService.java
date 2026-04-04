package com.seminary.sms.service;

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

    public List<Grade> getGradesByStudent(String studentId) {
        return gradeRepository.findByStudent_StudentId(studentId);
    }

    public List<Grade> getGradesByStudentAndSemester(String studentId, String semesterId) {
        return gradeRepository.findByStudent_StudentIdAndSemester_SemesterId(studentId, semesterId);
    }

    public List<Grade> getGradesBySemester(String semesterId) {
        return gradeRepository.findBySemester_SemesterId(semesterId);
    }

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

    @Transactional
    public Grade updateGrade(Integer gradeId, BigDecimal midterm, BigDecimal finalGrade,
                              Grade.GradeStatus status, String remarks, User enteredBy) {
        Grade grade = gradeRepository.findById(gradeId)
            .orElseThrow(() -> new RuntimeException("Grade not found: " + gradeId));
        grade.setMidtermGrade(midterm);
        grade.setFinalGrade(finalGrade);
        grade.setGradeStatus(status);
        grade.setRemarks(remarks);
        return saveGrade(grade, enteredBy);
    }
}

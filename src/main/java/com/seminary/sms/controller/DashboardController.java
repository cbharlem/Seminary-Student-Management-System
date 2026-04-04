package com.seminary.sms.controller;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final StudentRepository studentRepository;
    private final ApplicantRepository applicantRepository;
    private final CourseRepository courseRepository;
    private final AlumniRepository alumniRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SemesterRepository semesterRepository;
    private final ProgramRepository programRepository;

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasRole('Registrar')")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        long activeStudents = studentRepository.countByCurrentStatus(Student.StudentStatus.Active);
        long totalApplicants = applicantRepository.count();
        long activeCourses = courseRepository.findByIsActiveTrue().size();
        long totalAlumni = alumniRepository.count();

        stats.put("activeStudents", activeStudents);
        stats.put("totalApplicants", totalApplicants);
        stats.put("activeCourses", activeCourses);
        stats.put("totalAlumni", totalAlumni);

        // Recent enrollments
        semesterRepository.findByIsActiveTrue().ifPresent(sem -> {
            List<Enrollment> recent = enrollmentRepository.findBySemester_SemesterId(sem.getSemesterId());
            stats.put("recentEnrollments", recent.stream().limit(5).map(e -> Map.of(
                "enrollmentId", e.getEnrollmentId(),
                "studentName", e.getStudent().getFullName(),
                "studentId", e.getStudent().getStudentId(),
                "program", e.getProgram().getProgramCode(),
                "status", e.getEnrollmentStatus().name()
            )).toList());
            stats.put("activeSemester", sem.getSemesterLabel());
        });

        // Programs breakdown
        List<Program> programs = programRepository.findByIsActiveTrue();
        stats.put("programs", programs.stream().map(p -> Map.of(
            "programId", p.getProgramId(),
            "programCode", p.getProgramCode(),
            "programName", p.getProgramName(),
            "studentCount", studentRepository.countByProgram_ProgramId(p.getProgramId())
        )).toList());

        return ResponseEntity.ok(stats);
    }
}

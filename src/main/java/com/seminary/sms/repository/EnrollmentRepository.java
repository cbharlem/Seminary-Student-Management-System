package com.seminary.sms.repository;
import com.seminary.sms.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    Optional<Enrollment> findByEnrollmentId(String enrollmentId);
    Optional<Enrollment> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);
    Optional<Enrollment> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);
    List<Enrollment> findBySemester_Index(Integer semesterIndex);
    List<Enrollment> findBySemester_SemesterId(String semesterId);
    long countBySemester_SemesterId(String semesterId);
    List<Enrollment> findByStudent_Index(Integer studentIndex);
    List<Enrollment> findByStudent_StudentId(String studentId);
}

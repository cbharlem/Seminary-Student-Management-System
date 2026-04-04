package com.seminary.sms.repository;
import com.seminary.sms.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface GradeRepository extends JpaRepository<Grade, Integer> {
    Optional<Grade> findByGradeId(String gradeId);
    List<Grade> findByStudent_Index(Integer studentIndex);
    List<Grade> findByStudent_StudentId(String studentId);
    List<Grade> findBySemester_Index(Integer semesterIndex);
    List<Grade> findBySemester_SemesterId(String semesterId);
    List<Grade> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);
    List<Grade> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);
    Optional<Grade> findByEnrollmentSubject_Index(Integer enrollmentSubjectIndex);
    @Query("SELECT g FROM Grade g WHERE g.student.studentId = :studentId AND g.course.courseId = :courseId AND g.gradeStatus = :status")
    Optional<Grade> findPassedGrade(@Param("studentId") String studentId, @Param("courseId") String courseId, @Param("status") Grade.GradeStatus status);
}

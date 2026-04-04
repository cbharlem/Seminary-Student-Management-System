package com.seminary.sms.repository;
import com.seminary.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByStudentId(String studentId);
    boolean existsByStudentId(String studentId);
    boolean existsByApplication_ApplicationId(String applicationId);
    Optional<Student> findByEmail(String email);
    Optional<Student> findByUser_Index(Integer userIndex);
    Optional<Student> findByUser_UserId(String userId);
    List<Student> findByProgram_Index(Integer programIndex);
    List<Student> findByProgram_ProgramId(String programId);
    List<Student> findByCurrentStatus(Student.StudentStatus status);
    long countByCurrentStatus(Student.StudentStatus status);
    long countByProgram_ProgramId(String programId);
    List<Student> findByCurrentStatusAndProgram_ProgramId(Student.StudentStatus status, String programId);
    List<Student> findByProgram_IndexAndCurrentStatus(Integer programIndex, Student.StudentStatus status);
    // SECURITY (A03): Pageable limits results to prevent large query DoS attacks
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.studentId) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Student> searchStudents(String q, Pageable pageable);
}

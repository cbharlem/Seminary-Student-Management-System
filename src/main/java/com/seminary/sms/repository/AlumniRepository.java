package com.seminary.sms.repository;
import com.seminary.sms.entity.Alumni;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AlumniRepository extends JpaRepository<Alumni, Integer> {
    Optional<Alumni> findByStudent_Index(Integer studentIndex);
    Optional<Alumni> findByStudent_StudentId(String studentId);
    Optional<Alumni> findByAlumniId(String alumniId);
    boolean existsByAlumniId(String alumniId);
}

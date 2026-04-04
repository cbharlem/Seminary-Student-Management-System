package com.seminary.sms.repository;
import com.seminary.sms.entity.Instructor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {
    Optional<Instructor> findByInstructorId(String instructorId);
    boolean existsByInstructorId(String instructorId);
    List<Instructor> findByIsActiveTrue();
}

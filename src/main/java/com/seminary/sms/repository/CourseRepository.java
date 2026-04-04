package com.seminary.sms.repository;
import com.seminary.sms.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface CourseRepository extends JpaRepository<Course, Integer> {
    Optional<Course> findByCourseId(String courseId);
    boolean existsByCourseId(String courseId);
    List<Course> findByIsActiveTrue();
    List<Course> findByProgram_Index(Integer programIndex);
    List<Course> findByProgram_IndexAndIsActiveTrue(Integer programIndex);
    List<Course> findByProgram_ProgramIdAndIsActiveTrue(String programId);
}

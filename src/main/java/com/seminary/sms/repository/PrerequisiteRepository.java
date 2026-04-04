package com.seminary.sms.repository;
import com.seminary.sms.entity.Prerequisite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Integer> {
    List<Prerequisite> findByCourse_Index(Integer courseIndex);
    List<Prerequisite> findByCourse_CourseId(String courseId);
    void deleteByCourse_CourseId(String courseId);
    void deleteByPrerequisiteCourse_CourseId(String courseId);
}

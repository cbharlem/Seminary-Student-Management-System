package com.seminary.sms.repository;
import com.seminary.sms.entity.StudentSection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface StudentSectionRepository extends JpaRepository<StudentSection, Integer> {
    Optional<StudentSection> findByStudent_IndexAndSemester_Index(Integer studentIndex, Integer semesterIndex);
    Optional<StudentSection> findByStudent_StudentIdAndSemester_SemesterId(String studentId, String semesterId);
    List<StudentSection> findByStudent_Index(Integer studentIndex);
    List<StudentSection> findBySection_Index(Integer sectionIndex);
}

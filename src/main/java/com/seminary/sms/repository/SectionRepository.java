package com.seminary.sms.repository;
import com.seminary.sms.entity.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SectionRepository extends JpaRepository<Section, Integer> {
    Optional<Section> findBySectionId(String sectionId);
    List<Section> findBySemester_Index(Integer semesterIndex);
    List<Section> findBySemester_SemesterId(String semesterId);
    List<Section> findByProgram_Index(Integer programIndex);
}

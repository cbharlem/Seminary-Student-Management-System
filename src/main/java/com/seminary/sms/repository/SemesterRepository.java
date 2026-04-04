package com.seminary.sms.repository;
import com.seminary.sms.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;
public interface SemesterRepository extends JpaRepository<Semester, Integer> {
    Optional<Semester> findBySemesterId(String semesterId);
    Optional<Semester> findByIsActiveTrue();
    List<Semester> findBySchoolYear_Index(Integer schoolYearIndex);

    @Query("SELECT s FROM Semester s JOIN FETCH s.schoolYear WHERE s.isActive = true")
    Optional<Semester> findActiveWithSchoolYear();
}

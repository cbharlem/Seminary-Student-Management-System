package com.seminary.sms.repository;
import com.seminary.sms.entity.SchoolYear;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SchoolYearRepository extends JpaRepository<SchoolYear, Integer> {
    Optional<SchoolYear> findBySchoolYearId(String schoolYearId);
    Optional<SchoolYear> findByIsActiveTrue();
}

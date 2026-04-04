package com.seminary.sms.repository;
import com.seminary.sms.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ApplicationRepository extends JpaRepository<Application, Integer> {
    Optional<Application> findByApplicationId(String applicationId);
    List<Application> findByApplicant_Index(Integer applicantIndex);
    Optional<Application> findByApplicant_ApplicantId(String applicantId);
    List<Application> findByApplicationStatus(Application.ApplicationStatus status);
    List<Application> findBySchoolYear_Index(Integer schoolYearIndex);
}

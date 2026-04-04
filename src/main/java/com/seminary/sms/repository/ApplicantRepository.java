package com.seminary.sms.repository;
import com.seminary.sms.entity.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface ApplicantRepository extends JpaRepository<Applicant, Integer> {
    Optional<Applicant> findByApplicantId(String applicantId);
    boolean existsByApplicantId(String applicantId);
    Optional<Applicant> findByEmail(String email);
}

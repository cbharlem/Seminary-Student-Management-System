package com.seminary.sms.repository;
import com.seminary.sms.entity.EntranceExam;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EntranceExamRepository extends JpaRepository<EntranceExam, Integer> {
    List<EntranceExam> findByApplicant_Index(Integer applicantIndex);
    List<EntranceExam> findByApplicant_ApplicantId(String applicantId);
}

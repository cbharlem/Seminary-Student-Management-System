package com.seminary.sms.repository;
import com.seminary.sms.entity.EnrollmentSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EnrollmentSubjectRepository extends JpaRepository<EnrollmentSubject, Integer> {
    List<EnrollmentSubject> findByEnrollment_Index(Integer enrollmentIndex);
    List<EnrollmentSubject> findByEnrollment_EnrollmentId(String enrollmentId);
    List<EnrollmentSubject> findByEnrollment_Student_Index(Integer studentIndex);
}

package com.seminary.sms.repository;
import com.seminary.sms.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReportRepository extends JpaRepository<Report, Integer> {
    List<Report> findByStudent_Index(Integer studentIndex);
}

package com.seminary.sms.repository;
import com.seminary.sms.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    List<Document> findByStudent_Index(Integer studentIndex);
    List<Document> findByStudent_StudentId(String studentId);
}

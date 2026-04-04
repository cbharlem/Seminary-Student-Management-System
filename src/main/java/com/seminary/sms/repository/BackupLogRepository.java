package com.seminary.sms.repository;
import com.seminary.sms.entity.BackupLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BackupLogRepository extends JpaRepository<BackupLog, Integer> {
    List<BackupLog> findAllByOrderByBackupDateDesc();
}

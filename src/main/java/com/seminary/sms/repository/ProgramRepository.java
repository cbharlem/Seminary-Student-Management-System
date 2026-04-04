package com.seminary.sms.repository;
import com.seminary.sms.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ProgramRepository extends JpaRepository<Program, Integer> {
    Optional<Program> findByProgramId(String programId);
    Optional<Program> findByProgramCode(String programCode);
    List<Program> findByIsActiveTrue();
}

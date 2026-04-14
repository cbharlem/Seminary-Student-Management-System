package com.seminary.sms.repository;
import com.seminary.sms.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (StudentRepository)
// LAYER 3 → LAYER 4: Service calls methods here to fetch data from the database.
//   Spring reads the method name and auto-generates the SQL — no SQL written by hand.
// LAYER 5 → LAYER 4: Spring uses the Student entity (Layer 5) to know
//   what table and columns exist, so it can build the correct SQL.
// LAYER 4 → LAYER 3: Returns Java objects (Student, List<Student>, Optional<Student>)
//   which travel back up through Service → Controller → browser as JSON.
// ─────────────────────────────────────────────────────────────────────────────
public interface StudentRepository extends JpaRepository<Student, Integer> {

    // Auto-generates: SELECT * FROM tblstudents WHERE fldStudentID = ?
    // Returns Optional — the student may or may not exist
    // Called by: StudentService and StudentController to look up a student by their business ID
    Optional<Student> findByStudentId(String studentId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblstudents WHERE fldStudentID = ?
    // Called by: StudentController to check if a student ID already exists before creating a new record
    boolean existsByStudentId(String studentId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblstudents JOIN tblapplications ON ... WHERE fldApplicationID = ?
    // Called by: ApplicantService.admit() to verify no student record already exists for this application
    boolean existsByApplication_ApplicationId(String applicationId);

    // Auto-generates: SELECT * FROM tblstudents WHERE fldEmail = ?
    // Called by: duplicate-checking logic to prevent two students sharing the same email
    Optional<Student> findByEmail(String email);

    // Auto-generates: JOIN tblusers ON ... WHERE tblusers.fldIndex = ?
    // Called by: StudentService.getByUserId() to find the student linked to a logged-in user account (by PK)
    Optional<Student> findByUser_Index(Integer userIndex);

    // Auto-generates: JOIN tblusers ON ... WHERE tblusers.fldUserID = ?
    // Called by: StudentMeController and MeController to identify which student the current user belongs to
    Optional<Student> findByUser_UserId(String userId);

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldIndex = ?
    // LAYER 5 → LAYER 4: Uses @ManyToOne on Student.program to know how to JOIN
    // Called by: lookups that filter students by program (by PK)
    List<Student> findByProgram_Index(Integer programIndex);

    // Auto-generates: JOIN tblprogram ON ... WHERE tblprogram.fldProgramID = ?
    // Called by: DashboardController and StudentController to list or count students in a program
    List<Student> findByProgram_ProgramId(String programId);

    // Auto-generates: SELECT * FROM tblstudents WHERE fldCurrentStatus = ?
    // Called by: StudentController.getAll() when filtering the student list by status
    List<Student> findByCurrentStatus(Student.StudentStatus status);

    // Auto-generates: SELECT COUNT(*) FROM tblstudents WHERE fldCurrentStatus = ?
    // Called by: DashboardController.getStats() to count students by enrollment status
    long countByCurrentStatus(Student.StudentStatus status);

    // Auto-generates: SELECT COUNT(*) FROM tblstudents JOIN tblprogram ... WHERE fldProgramID = ?
    // Called by: DashboardController.getStats() to count how many students belong to each program
    long countByProgram_ProgramId(String programId);

    // Auto-generates: WHERE fldCurrentStatus = ? AND tblprogram.fldProgramID = ?
    // Called by: StudentController to filter students by both status and program at the same time
    List<Student> findByCurrentStatusAndProgram_ProgramId(Student.StudentStatus status, String programId);

    // Auto-generates: JOIN tblprogram ... WHERE tblprogram.fldIndex = ? AND fldCurrentStatus = ?
    // Called by: lookups that filter students by program PK and status together
    List<Student> findByProgram_IndexAndCurrentStatus(Integer programIndex, Student.StudentStatus status);

    // Too complex for auto-generation — manually written JPQL query
    // Searches firstName, lastName, and studentId all at once (case-insensitive)
    // LAYER 3 → LAYER 4: Called by StudentService.searchStudents()
    // SECURITY (A03): Pageable limits results to prevent large query DoS attacks
    @Query("SELECT s FROM Student s WHERE LOWER(s.firstName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.lastName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(s.studentId) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Student> searchStudents(String q, Pageable pageable);
}

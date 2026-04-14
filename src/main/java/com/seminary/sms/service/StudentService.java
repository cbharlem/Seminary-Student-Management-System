package com.seminary.sms.service;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (StudentService)
// LAYER 2 → LAYER 3: The Controller calls methods here when business logic is needed
//   before hitting the database (e.g. search, create with account, update status).
// LAYER 3 → LAYER 4: Each method here calls StudentRepository (Layer 4)
//   to actually talk to the database.
// LAYER 4 → LAYER 3 → LAYER 2: Repository returns data, Service processes it
//   if needed, then passes it back up to the Controller.
// ─────────────────────────────────────────────────────────────────────────────
@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class StudentService {

    // LAYER 3 → LAYER 4: All database access goes through this repository
    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // LAYER 2 → LAYER 3 → LAYER 4: Controller asks for all students,
    //   Service forwards to Repository which runs SELECT * FROM tblstudents
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // LAYER 3 → LAYER 4: Passes status filter down to Repository
    public List<Student> getActiveStudents() {
        return studentRepository.findByCurrentStatus(Student.StudentStatus.Active);
    }

    // LAYER 3 → LAYER 4: Passes student ID down to Repository
    // LAYER 4 → LAYER 3: Returns Optional<Student> — present if found, empty if not
    public Optional<Student> getById(String id) {
        return studentRepository.findByStudentId(id);
    }

    // LAYER 2 → LAYER 3: Called by StudentMeController to link a logged-in user account to their student record
    // LAYER 3 → LAYER 4: Calls studentRepository.findByUser_UserId()
    // LAYER 4 → LAYER 3 → LAYER 2: Returns Optional<Student> — present if a linked student record exists
    public Optional<Student> getByUserId(String userId) {
        return studentRepository.findByUser_UserId(userId);
    }

    // LAYER 2 → LAYER 3: Controller sends search term here
    // LAYER 3 → LAYER 4: Passes query to Repository which runs a LIKE SQL query
    // LAYER 4 → LAYER 3 → LAYER 2 → LAYER 1: Returns matching students back up the chain
    public List<Student> searchStudents(String query) {
        // SECURITY (A03): Cap search results at 50 to prevent large query DoS
        return studentRepository.searchStudents(query, PageRequest.of(0, 50));
    }

    // LAYER 2 → LAYER 3: Called by DashboardController to count active students for the stats tile
    // LAYER 3 → LAYER 4: Calls countByCurrentStatus() — runs SELECT COUNT(*) with a WHERE filter
    public long countActive() {
        return studentRepository.countByCurrentStatus(Student.StudentStatus.Active);
    }

    // LAYER 2 → LAYER 3: Called by StudentController.create() and StudentController.update() after resolving references
    // LAYER 3 → LAYER 4: Calls studentRepository.save() which issues INSERT or UPDATE in the database
    // @Transactional ensures the whole save either completes fully or is rolled back on error
    @Transactional
    public Student save(Student student) {
        return studentRepository.save(student);
    }

    // SECURITY (A07): Generate a cryptographically random temporary password.
    // The caller (admit endpoint) must communicate this to the registrar securely.
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // LAYER 2 → LAYER 3: Called by ApplicantController.admit() to create both a Student record and a login account
    // LAYER 3 → LAYER 4: Saves a new User (with hashed temp password) then links it to the Student and saves both
    // @Transactional ensures both saves succeed together — if the user save fails, the student save is also rolled back
    // Returns the plain-text temporary password once so the controller can pass it to the registrar
    @Transactional
    public String createWithAccount(Student student, String username) {
        // SECURITY (A07): Never use studentId as password — generate random temp password
        String tempPassword = generateTemporaryPassword();
        String userId = "USR-" + String.format("%04d", 1001 + userRepository.count());
        User user = User.builder()
            .userId(userId)
            .username(username)
            .passwordHash(passwordEncoder.encode(tempPassword))
            .role(User.Role.Student)
            .isActive(true)
            .build();
        user = userRepository.save(user);
        student.setUser(user);
        studentRepository.save(student);
        // Return the plain-text temp password once so the registrar can hand it to the student
        return tempPassword;
    }

    // LAYER 2 → LAYER 3: Called by StudentController.updateStatus() when the registrar changes a student's status
    // LAYER 3 → LAYER 4: Fetches the student by ID, updates the status field, then saves
    // @Transactional ensures the update is atomic — either the status is saved or nothing changes
    @Transactional
    public void updateStatus(String studentId, Student.StudentStatus status) {
        Student s = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        s.setCurrentStatus(status);
        studentRepository.save(s);
    }

    // NOTE: Student records are NEVER deleted per seminary data retention policy.
}

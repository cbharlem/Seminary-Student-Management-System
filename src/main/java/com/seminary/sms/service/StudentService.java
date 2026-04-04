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

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    public List<Student> getActiveStudents() {
        return studentRepository.findByCurrentStatus(Student.StudentStatus.Active);
    }

    public Optional<Student> getById(String id) {
        return studentRepository.findByStudentId(id);
    }

    public Optional<Student> getByUserId(String userId) {
        return studentRepository.findByUser_UserId(userId);
    }

    public List<Student> searchStudents(String query) {
        // SECURITY (A03): Cap search results at 50 to prevent large query DoS
        return studentRepository.searchStudents(query, PageRequest.of(0, 50));
    }

    public long countActive() {
        return studentRepository.countByCurrentStatus(Student.StudentStatus.Active);
    }

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

    @Transactional
    public void updateStatus(String studentId, Student.StudentStatus status) {
        Student s = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
        s.setCurrentStatus(status);
        studentRepository.save(s);
    }

    // NOTE: Student records are NEVER deleted per seminary data retention policy.
}

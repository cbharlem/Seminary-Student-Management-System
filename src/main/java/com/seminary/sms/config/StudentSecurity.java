package com.seminary.sms.config;

import com.seminary.sms.repository.StudentRepository;
import com.seminary.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Used in @PreAuthorize("@studentSecurity.isOwner(authentication, #studentId)")
 * Allows a Student user to access only their own records.
 */
@Component("studentSecurity")
@RequiredArgsConstructor
public class StudentSecurity {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;

    public boolean isOwner(Authentication auth, String studentId) {
        if (auth == null || studentId == null) return false;
        // Registrar can always access
        boolean isRegistrar = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_Registrar"));
        if (isRegistrar) return true;
        // Student can only access their own record
        return userRepository.findByUsername(auth.getName())
            .flatMap(u -> studentRepository.findByUser_UserId(u.getUserId()))
            .map(s -> s.getStudentId().equals(studentId))
            .orElse(false);
    }
}

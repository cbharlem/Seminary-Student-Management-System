package com.seminary.sms.config;

// ─────────────────────────────────────────────────────────────────────────────
// CONFIG / SUPPORT — StudentSecurity
// This is not part of any numbered layer. It is a security helper component
// used in method-level access control expressions.
//
// What it does:
//   Some API endpoints should only be accessible by the student who owns the data
//   (e.g., a student should not be able to view another student's grades).
//   Spring Security's @PreAuthorize annotation allows custom expressions like:
//
//       @PreAuthorize("@studentSecurity.isOwner(authentication, #studentId)")
//
//   This calls the isOwner() method below before allowing the request through.
//
// How isOwner() works:
//   1. If the user is a Registrar → always allowed (returns true).
//   2. If the user is a Student → looks up their linked Student record
//      and checks whether their studentId matches the one in the URL.
//      If it matches → allowed. If not → denied (returns false).
//
// This prevents Insecure Direct Object Reference (IDOR) attacks where
// one student could guess another student's ID and access their records.
// ─────────────────────────────────────────────────────────────────────────────

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
        // Admin and Registrar can always access any student record
        boolean isAdminOrRegistrar = auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_Admin") || a.getAuthority().equals("ROLE_Registrar"));
        if (isAdminOrRegistrar) return true;
        // Student can only access their own record
        return userRepository.findByUsername(auth.getName())
            .flatMap(u -> studentRepository.findByUser_UserId(u.getUserId()))
            .map(s -> s.getStudentId().equals(studentId))
            .orElse(false);
    }
}

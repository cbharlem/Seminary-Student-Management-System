package com.seminary.sms.controller;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 2 — CONTROLLER (MeController)
// Handles URL group: /api/me
// Available to any authenticated user (both Registrar and Student).
//
// These endpoints allow the currently logged-in user to manage their own account.
// The identity of the current user is read from the Authentication object that
// Spring Security automatically injects — no user ID is passed in the URL,
// which prevents one user from accessing another user's profile (IDOR prevention).
//
// Endpoints handled:
//   GET  /api/me               → returns the logged-in user's userId, username, role
//   GET  /api/me/photo         → returns the user's profile picture as image bytes
//   POST /api/me/photo         → uploads/replaces the profile picture (images only, max 2 MB)
//   PATCH /api/me/username     → changes the user's own username (checks for duplicates)
//   PATCH /api/me/password     → changes the user's password (requires current password,
//                                enforces complexity: length, uppercase, lowercase, number, symbol)
//
// Repositories used:
//   UserRepository — to look up and save the current user's record
//
// LAYER 2 → LAYER 4: Calls UserRepository directly (no service needed — logic is simple).
// LAYER 2 → LAYER 1: Returns JSON responses consumed by the profile page in the frontend.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * MeController
 *
 * Endpoints for the currently authenticated user:
 *   GET  /api/me                  — who am I (role, username, userId)
 *   GET  /api/me/photo            — get profile picture bytes
 *   POST /api/me/photo            — upload / replace profile picture
 *   PATCH /api/me/username        — change own username
 *   PATCH /api/me/password        — change own password (requires current password)
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // LAYER 1 → LAYER 2: Triggered by app.js init() immediately on page load to identify the logged-in user
    // LAYER 2 → LAYER 4: Uses Spring Security's Authentication object to find the username, then calls UserRepository
    // LAYER 2 → LAYER 1: Returns userId, username, role, and isActive — the frontend stores this in SMS.currentUser
    // ── Who am I ─────────────────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        return userRepository.findByUsername(auth.getName())
            .map(user -> ResponseEntity.ok(Map.of(
                "userId",   user.getUserId(),
                "username", user.getUsername(),
                "role",     user.getRole().name(),
                "isActive", user.getIsActive()
            )))
            .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js init() and loadMyProfile() to display the user's profile picture
    // LAYER 2 → LAYER 4: Fetches the User record, reads the profilePicture blob field
    // LAYER 2 → LAYER 1: Returns the raw image bytes with the correct Content-Type header (e.g., image/jpeg)
    // ── Profile Picture — GET ─────────────────────────────────────
    @GetMapping("/me/photo")
    public ResponseEntity<byte[]> getPhoto(Authentication auth) {
        var userOpt = userRepository.findByUsername(auth.getName());
        if (userOpt.isEmpty()) return ResponseEntity.status(401).build();
        var user = userOpt.get();
        if (user.getProfilePicture() == null || user.getProfilePicture().length == 0)
            return ResponseEntity.notFound().build();
        String ct = user.getProfilePictureType() != null
            ? user.getProfilePictureType() : "image/jpeg";
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(ct))
            .body(user.getProfilePicture());
    }

    // LAYER 1 → LAYER 2: Triggered by app.js uploadProfilePic() when the user selects a new photo
    // LAYER 2 → LAYER 4: Validates file type and size, then stores the bytes in the User entity's profilePicture field
    // LAYER 2 → LAYER 1: Returns a success message, or 400 if the file type or size is invalid
    // ── Profile Picture — POST ────────────────────────────────────
    @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadPhoto(Authentication auth,
                                         @RequestParam("photo") MultipartFile file) {
        if (file.isEmpty())
            return ResponseEntity.badRequest().body(Map.of("error", "No file selected."));

        String contentType = file.getContentType();
        // SECURITY (A03): Only allow image MIME types to prevent upload of malicious files
        if (contentType == null || !contentType.startsWith("image/"))
            return ResponseEntity.badRequest().body(Map.of("error", "Only image files are allowed."));

        // SECURITY (A05): Cap file size at 2 MB
        if (file.getSize() > 2L * 1024 * 1024)
            return ResponseEntity.badRequest().body(Map.of("error", "Image must be smaller than 2 MB."));

        return userRepository.findByUsername(auth.getName())
            .map(user -> {
                try {
                    user.setProfilePicture(file.getBytes());
                    user.setProfilePictureType(contentType);
                    userRepository.save(user);
                    return ResponseEntity.ok(Map.of("message", "Profile photo updated."));
                } catch (Exception e) {
                    return ResponseEntity.status(500).body(Map.of("error", "Failed to save photo."));
                }
            })
            .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js changeMyUsername() when the user submits the username change form
    // LAYER 2 → LAYER 4: Checks for duplicates, then updates the username in the User record via UserRepository
    // LAYER 2 → LAYER 1: Returns a success message reminding the user to re-login, or 400 if the name is taken
    // ── Change Username ───────────────────────────────────────────
    @PatchMapping("/me/username")
    public ResponseEntity<?> changeUsername(Authentication auth,
                                            @RequestBody Map<String, String> body) {
        String newUsername = body.get("username");
        if (newUsername == null || newUsername.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "Username cannot be empty."));
        if (newUsername.length() > 50)
            return ResponseEntity.badRequest().body(Map.of("error", "Username must be 50 characters or fewer."));

        // SECURITY (A01): Check uniqueness before saving
        if (userRepository.existsByUsername(newUsername))
            return ResponseEntity.badRequest().body(Map.of("error", "That username is already taken."));

        return userRepository.findByUsername(auth.getName())
            .map(user -> {
                user.setUsername(newUsername);
                userRepository.save(user);
                // Session still uses old username — user must re-login for it to take effect
                return ResponseEntity.ok(Map.of(
                    "message", "Username updated. Please log out and log back in.",
                    "newUsername", newUsername));
            })
            .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
    }

    // LAYER 1 → LAYER 2: Triggered by app.js changeMyPassword() when the user submits the password change form
    // LAYER 2 → LAYER 4: Verifies the current password with BCrypt, enforces complexity rules, then saves the new hash
    // LAYER 2 → LAYER 1: Returns a success message, or 400 if the current password is wrong or complexity rules fail
    // ── Change Password ───────────────────────────────────────────
    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(Authentication auth,
                                            @RequestBody Map<String, String> body) {
        String currentPw = body.get("currentPassword");
        String newPw     = body.get("newPassword");

        if (currentPw == null || newPw == null || currentPw.isBlank() || newPw.isBlank())
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required."));

        // SECURITY (A07): Enforce complexity rules
        if (newPw.length() < 8)
            return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 8 characters."));
        if (!newPw.matches(".*[A-Z].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least one uppercase letter."));
        if (!newPw.matches(".*[a-z].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least one lowercase letter."));
        if (!newPw.matches(".*[0-9].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least one number."));
        if (!newPw.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))
            return ResponseEntity.badRequest().body(Map.of("error", "Password must contain at least one special character."));

        return userRepository.findByUsername(auth.getName())
            .map(user -> {
                // SECURITY (A07): Verify identity — must supply correct current password
                if (!passwordEncoder.matches(currentPw, user.getPasswordHash()))
                    return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect."));

                user.setPasswordHash(passwordEncoder.encode(newPw));
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
            })
            .orElse(ResponseEntity.status(401).body(Map.of("error", "User not found")));
    }
}

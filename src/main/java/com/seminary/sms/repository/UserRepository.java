package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (UserRepository)
// Serves the User entity — reads from and writes to the tblusers table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByUsername      → used by Spring Security at login to load the user's account
//   findByUserId        → finds a user by their business ID (used in profile and admin endpoints)
//   existsByUsername    → returns true/false — used to prevent duplicate usernames
//
// LAYER 4 → LAYER 5: Uses the User entity to map database rows to objects.
// LAYER 4 → LAYER 2: MeController and UserController call this for account management.
// LAYER 4 — Config:  SecurityConfig uses this repository inside the UserDetailsService
//                    bean to authenticate users at login time.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Integer> {

    // Auto-generates: SELECT * FROM tblusers WHERE fldUsername = ?
    // Called by: SecurityConfig's UserDetailsService at login time to load the user's credentials for authentication
    Optional<User> findByUsername(String username);

    // Auto-generates: SELECT * FROM tblusers WHERE fldUserID = ?
    // Called by: MeController and UserController to fetch a user account by its business ID
    Optional<User> findByUserId(String userId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblusers WHERE fldUsername = ?
    // Called by: UserController.create() to prevent creating two accounts with the same username
    boolean existsByUsername(String username);
}

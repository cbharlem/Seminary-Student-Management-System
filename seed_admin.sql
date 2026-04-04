-- ============================================================
-- SMS Initial Setup Script
-- St. Francis de Sales Major Seminary
-- Run this AFTER importing the main database schema.
-- ============================================================

-- Default Registrar account (change password immediately after first login)
INSERT INTO tblusers (fldUserID, fldUsername, fldPasswordHash, fldRole, fldIsActive, fldCreatedAt, fldUpdatedAt)
VALUES (
  'USR-admin',
  'admin',
  '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lkii',
  'Registrar',
  1,
  NOW(),
  NOW()
)
ON DUPLICATE KEY UPDATE fldUpdatedAt = NOW();

-- ⚠️  IMPORTANT: Change the admin password immediately after first login
-- via System > User Accounts > Reset Password in the application.

-- If the sample data already has tblusers entries, check existing users:
-- SELECT fldUserID, fldUsername, fldRole, fldIsActive FROM tblusers;

-- To generate a new BCrypt hash for a custom password, use:
-- https://bcrypt-generator.com (10 rounds) or run in Java:
-- new BCryptPasswordEncoder().encode("yourPassword");

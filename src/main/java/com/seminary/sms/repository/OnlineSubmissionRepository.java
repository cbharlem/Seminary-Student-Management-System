package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (OnlineSubmissionRepository)
// Provides database access for the OnlineSubmission entity (tblonline_submissions).
//
// LAYER 3 → LAYER 4: OnlineSubmissionController calls these methods to query
//   or persist online submissions.
// LAYER 4 → LAYER 5: Returns OnlineSubmission entity objects.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.OnlineSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OnlineSubmissionRepository extends JpaRepository<OnlineSubmission, Integer> {

    /** Find a submission by its human-readable ID (e.g. "SUB-1001"). */
    Optional<OnlineSubmission> findBySubmissionId(String submissionId);

    /** List all submissions with a given status, newest first. */
    List<OnlineSubmission> findByStatusOrderBySubmittedAtDesc(OnlineSubmission.SubmissionStatus status);

    /** List all submissions regardless of status, newest first. */
    List<OnlineSubmission> findAllByOrderBySubmittedAtDesc();

    /** Prevent duplicate pending submissions from the same email address. */
    boolean existsByEmailAndStatus(String email, OnlineSubmission.SubmissionStatus status);

    /**
     * Returns the highest numeric suffix used in any submissionId so far.
     * Used to generate the next SUB-#### value.
     * Returns null if the table is empty.
     */
    @Query("SELECT MAX(CAST(SUBSTRING(s.submissionId, 5) AS integer)) FROM OnlineSubmission s")
    Integer findMaxSubmissionIdNumber();
}

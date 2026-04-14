package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (ScheduleService)
// Handles all business logic for creating and managing class schedules,
// including detecting time conflicts before saving.
//
// Repositories used:
//   ScheduleRepository — to save, retrieve, and delete schedule records,
//                        and to run conflict-detection queries
//
// Business logic handled here:
//   - detectConflicts()  → checks two things before allowing a schedule to be saved:
//                          1. Is the room already booked at this day/time?
//                          2. Does the instructor already have a class at this day/time?
//                          Returns a list of conflict messages (empty = no conflicts).
//                          When editing, it excludes the current schedule from the check
//                          so a schedule doesn't conflict with itself.
//   - save()             → runs detectConflicts() first, then saves if clear.
//   - update()           → runs detectConflicts() excluding the existing schedule, then saves.
//   - delete()           → removes a schedule by its business ID.
//
// LAYER 3 → LAYER 4: Calls ScheduleRepository to run conflict queries and save/delete schedules.
// LAYER 4 → LAYER 3: Repository returns Schedule objects and conflict result lists.
// LAYER 3 → LAYER 2: ScheduleController calls this service and returns results to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Schedule;
import com.seminary.sms.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    // LAYER 2 → LAYER 3: Called by ScheduleController.getAll() and ScheduleMeController to get schedules for a section
    // LAYER 3 → LAYER 4: Calls scheduleRepository.findBySection_SectionId()
    public List<Schedule> getBySection(String sectionId) {
        return scheduleRepository.findBySection_SectionId(sectionId);
    }

    // LAYER 2 → LAYER 3: Called internally by save() and update() before committing a schedule
    // LAYER 3 → LAYER 4: Runs two custom JPQL queries — one for room conflicts, one for instructor conflicts
    //   If excludeScheduleId is provided (during an edit), that schedule is skipped so it doesn't conflict with itself
    // Returns a list of conflict messages — empty list means no conflicts
    public List<String> detectConflicts(Schedule candidate, String excludeScheduleId) {
        List<String> conflicts = new ArrayList<>();

        List<Schedule> roomConflicts = (excludeScheduleId != null)
            ? scheduleRepository.findConflictsByRoomExcluding(
                candidate.getRoom().getRoomId(), excludeScheduleId,
                candidate.getDayOfWeek(), candidate.getTimeStart(), candidate.getTimeEnd())
            : scheduleRepository.findConflictsByRoom(
                candidate.getRoom().getRoomId(),
                candidate.getDayOfWeek(), candidate.getTimeStart(), candidate.getTimeEnd());
        if (!roomConflicts.isEmpty())
            conflicts.add("Room '" + candidate.getRoom().getRoomName() + "' is already booked at this time.");

        List<Schedule> instrConflicts = (excludeScheduleId != null)
            ? scheduleRepository.findConflictsByInstructorExcluding(
                candidate.getInstructor().getInstructorId(), excludeScheduleId,
                candidate.getDayOfWeek(), candidate.getTimeStart(), candidate.getTimeEnd())
            : scheduleRepository.findConflictsByInstructor(
                candidate.getInstructor().getInstructorId(),
                candidate.getDayOfWeek(), candidate.getTimeStart(), candidate.getTimeEnd());
        if (!instrConflicts.isEmpty())
            conflicts.add("Instructor '" + candidate.getInstructor().getFullName() + "' has another class at this time.");

        return conflicts;
    }

    // LAYER 2 → LAYER 3: Called by ScheduleController.create() to save a new class schedule
    // LAYER 3 → LAYER 4: Runs detectConflicts() first — throws if any conflict found; otherwise saves via repository
    @Transactional
    public Schedule save(Schedule schedule) {
        List<String> conflicts = detectConflicts(schedule, null);
        if (!conflicts.isEmpty())
            throw new RuntimeException("Scheduling conflict: " + String.join(" | ", conflicts));
        return scheduleRepository.save(schedule);
    }

    // LAYER 2 → LAYER 3: Called by ScheduleController.update() to edit an existing schedule
    // LAYER 3 → LAYER 4: Runs detectConflicts() excluding the current schedule, then saves via repository
    @Transactional
    public Schedule update(Schedule schedule, String existingScheduleId) {
        List<String> conflicts = detectConflicts(schedule, existingScheduleId);
        if (!conflicts.isEmpty())
            throw new RuntimeException("Scheduling conflict: " + String.join(" | ", conflicts));
        return scheduleRepository.save(schedule);
    }

    // LAYER 2 → LAYER 3: Called by ScheduleController.delete() to remove a schedule by its business ID
    // LAYER 3 → LAYER 4: Calls scheduleRepository.deleteByScheduleId() — @Transactional required for delete queries
    @Transactional
    public void delete(String scheduleId) {
        scheduleRepository.deleteByScheduleId(scheduleId);
    }
}

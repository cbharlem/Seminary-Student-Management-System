package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (ScheduleRepository)
// Serves the Schedule entity — reads from and writes to the tblschedule table.
//
// Standard method-name queries:
//   findByScheduleId          → finds one schedule by its business ID
//   findBySection_Index/Id    → all schedules for a section
//   findByInstructor_Index    → all schedules assigned to one instructor
//   deleteByScheduleId        → deletes a schedule by its business ID (@Transactional required)
//
// Custom @Query methods (written in JPQL — these check for time overlaps):
//   findRoomConflicts          → finds other schedules in the same room, on the same day,
//                                whose time overlaps with the proposed slot
//   findInstructorConflicts    → same check but for the instructor
//   findConflictsByRoom/Instructor         → same as above but using business IDs
//   findConflictsByRoomExcluding/...       → same checks but excludes one schedule ID
//                                            (used when editing an existing schedule)
//
// LAYER 4 → LAYER 5: Uses the Schedule entity to map database rows to objects.
// LAYER 4 → LAYER 3: ScheduleService calls this to detect conflicts before saving.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {

    // Auto-generates: SELECT * FROM tblschedule WHERE fldScheduleID = ?
    // Called by: ScheduleService and ScheduleController to fetch a specific schedule entry
    Optional<Schedule> findByScheduleId(String scheduleId);

    // Auto-generates: JOIN tblsection ON ... WHERE tblsection.fldIndex = ?
    // Called by: ScheduleService to get all schedules for a section (by PK)
    List<Schedule> findBySection_Index(Integer sectionIndex);

    // Auto-generates: JOIN tblsection ON ... WHERE tblsection.fldSectionID = ?
    // Called by: ScheduleController.getBySection() to list all schedule slots for a given section
    List<Schedule> findBySection_SectionId(String sectionId);

    // Auto-generates: JOIN tblinstructors ON ... WHERE tblinstructors.fldIndex = ?
    // Called by: ScheduleService to retrieve all schedules assigned to an instructor (by PK)
    List<Schedule> findByInstructor_Index(Integer instructorIndex);

    // Auto-generates: DELETE FROM tblschedule WHERE fldScheduleID = ?
    // @Transactional required — Spring needs an active transaction to run a DELETE by derived query
    // Called by: ScheduleController.delete() to remove a schedule entry
    @Transactional
    void deleteByScheduleId(String scheduleId);

    // Custom JPQL: finds other schedules using the same room on the same day whose time overlaps
    // Overlap condition: existing slot starts before the new end AND existing slot ends after the new start
    // Called by: ScheduleService.checkConflicts() when creating a new schedule (using room PK)
    @Query("SELECT s FROM Schedule s WHERE s.room.index = :roomIndex AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findRoomConflicts(@Param("roomIndex") Integer roomIndex, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);

    // Custom JPQL: same overlap check but for an instructor instead of a room (uses instructor PK)
    // Called by: ScheduleService.checkConflicts() to prevent double-booking an instructor
    @Query("SELECT s FROM Schedule s WHERE s.instructor.index = :instructorIndex AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findInstructorConflicts(@Param("instructorIndex") Integer instructorIndex, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);

    // Custom JPQL: room conflict check using the room's business ID instead of its PK
    // Called by: ScheduleService when creating a schedule using room ID strings
    @Query("SELECT s FROM Schedule s WHERE s.room.roomId = :roomId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByRoom(@Param("roomId") String roomId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);

    // Custom JPQL: instructor conflict check using the instructor's business ID
    // Called by: ScheduleService when creating a schedule using instructor ID strings
    @Query("SELECT s FROM Schedule s WHERE s.instructor.instructorId = :instructorId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByInstructor(@Param("instructorId") String instructorId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);

    // Custom JPQL: room conflict check that excludes one specific schedule ID
    // Used when editing an existing schedule so it does not flag itself as a conflict
    @Query("SELECT s FROM Schedule s WHERE s.room.roomId = :roomId AND s.scheduleId <> :excludeId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByRoomExcluding(@Param("roomId") String roomId, @Param("excludeId") String excludeId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);

    // Custom JPQL: instructor conflict check that excludes one schedule ID (same self-exclusion logic)
    // Called by: ScheduleService.update() to allow saving a schedule to the same slot it already occupies
    @Query("SELECT s FROM Schedule s WHERE s.instructor.instructorId = :instructorId AND s.scheduleId <> :excludeId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByInstructorExcluding(@Param("instructorId") String instructorId, @Param("excludeId") String excludeId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
}

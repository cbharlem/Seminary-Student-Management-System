package com.seminary.sms.service;

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

    public List<Schedule> getBySection(String sectionId) {
        return scheduleRepository.findBySection_SectionId(sectionId);
    }

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

    @Transactional
    public Schedule save(Schedule schedule) {
        List<String> conflicts = detectConflicts(schedule, null);
        if (!conflicts.isEmpty())
            throw new RuntimeException("Scheduling conflict: " + String.join(" | ", conflicts));
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public Schedule update(Schedule schedule, String existingScheduleId) {
        List<String> conflicts = detectConflicts(schedule, existingScheduleId);
        if (!conflicts.isEmpty())
            throw new RuntimeException("Scheduling conflict: " + String.join(" | ", conflicts));
        return scheduleRepository.save(schedule);
    }

    @Transactional
    public void delete(String scheduleId) {
        scheduleRepository.deleteByScheduleId(scheduleId);
    }
}

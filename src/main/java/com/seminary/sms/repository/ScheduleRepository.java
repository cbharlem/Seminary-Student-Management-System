package com.seminary.sms.repository;
import com.seminary.sms.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
public interface ScheduleRepository extends JpaRepository<Schedule, Integer> {
    Optional<Schedule> findByScheduleId(String scheduleId);
    List<Schedule> findBySection_Index(Integer sectionIndex);
    List<Schedule> findBySection_SectionId(String sectionId);
    List<Schedule> findByInstructor_Index(Integer instructorIndex);
    @Transactional
    void deleteByScheduleId(String scheduleId);
    @Query("SELECT s FROM Schedule s WHERE s.room.index = :roomIndex AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findRoomConflicts(@Param("roomIndex") Integer roomIndex, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
    @Query("SELECT s FROM Schedule s WHERE s.instructor.index = :instructorIndex AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findInstructorConflicts(@Param("instructorIndex") Integer instructorIndex, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
    @Query("SELECT s FROM Schedule s WHERE s.room.roomId = :roomId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByRoom(@Param("roomId") String roomId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
    @Query("SELECT s FROM Schedule s WHERE s.instructor.instructorId = :instructorId AND s.dayOfWeek = :day AND ((s.timeStart < :end) AND (s.timeEnd > :start))")
    List<Schedule> findConflictsByInstructor(@Param("instructorId") String instructorId, @Param("day") Schedule.DayOfWeek day, @Param("start") java.time.LocalTime start, @Param("end") java.time.LocalTime end);
}

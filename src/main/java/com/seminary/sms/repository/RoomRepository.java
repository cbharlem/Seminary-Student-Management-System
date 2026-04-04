package com.seminary.sms.repository;
import com.seminary.sms.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByRoomId(String roomId);
    boolean existsByRoomId(String roomId);
    List<Room> findByIsActiveTrue();
}

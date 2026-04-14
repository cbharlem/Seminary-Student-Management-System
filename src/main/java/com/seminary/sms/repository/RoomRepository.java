package com.seminary.sms.repository;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 4 — REPOSITORY (RoomRepository)
// Serves the Room entity — reads from and writes to the tblrooms table.
//
// Spring auto-generates SQL from the method names declared here:
//   findByRoomId      → finds one room by its business ID
//   existsByRoomId    → returns true/false — used to check before an update
//   findByIsActiveTrue → returns only active rooms (for schedule assignment dropdowns)
//
// LAYER 4 → LAYER 5: Uses the Room entity to map database rows to objects.
// LAYER 4 → LAYER 2: SectionController calls this to manage room records.
// LAYER 4 → LAYER 3: ScheduleService uses rooms when checking for scheduling conflicts.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface RoomRepository extends JpaRepository<Room, Integer> {

    // Auto-generates: SELECT * FROM tblrooms WHERE fldRoomID = ?
    // Called by: SectionController and ScheduleService to fetch a specific room record
    Optional<Room> findByRoomId(String roomId);

    // Auto-generates: SELECT COUNT(*) > 0 FROM tblrooms WHERE fldRoomID = ?
    // Called by: SectionController.updateRoom() to verify the room exists before saving changes
    boolean existsByRoomId(String roomId);

    // Auto-generates: SELECT * FROM tblrooms WHERE fldIsActive = 1
    // Called by: SectionController.getRooms() to populate the room dropdown for schedule assignment
    List<Room> findByIsActiveTrue();
}

package com.seminary.sms.service;

// ─────────────────────────────────────────────────────────────────────────────
// LAYER 3 — SERVICE (AlumniService)
// Handles all business logic related to alumni records.
//
// This service sits between the controller (Layer 2) and the repositories (Layer 4).
// Controllers should never talk directly to repositories for complex operations —
// they ask the service instead, and the service decides what to do.
//
// Repositories used:
//   AlumniRepository   — to save and retrieve alumni records
//   StudentRepository  — to look up the student being graduated and update their status
//
// Business logic handled here:
//   - graduateStudent()  → changes a student's status to Alumni, then creates an Alumni record.
//                          Uses @Transactional so both saves either succeed together or both fail.
//   - unmarkAlumni()     → reverses the graduation: deletes the Alumni record, then
//                          sets the student's status back to Active.
//   - update()           → saves changes to an existing alumni record.
//
// LAYER 3 → LAYER 4: Calls AlumniRepository and StudentRepository to read/write the database.
// LAYER 4 → LAYER 3: Repositories return Alumni and Student objects.
// LAYER 3 → LAYER 2: AlumniController calls this service and sends the result to the browser.
// ─────────────────────────────────────────────────────────────────────────────

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class AlumniService {

    private final AlumniRepository alumniRepository;
    private final StudentRepository studentRepository;

    // LAYER 2 → LAYER 3: Called by AlumniController.getAll() to list all alumni records
    // LAYER 3 → LAYER 4: Calls alumniRepository.findAll() — returns every row in tblalumni
    public List<Alumni> getAll() {
        return alumniRepository.findAll();
    }

    // LAYER 2 → LAYER 3: Utility method to look up an alumni record by the linked student's ID
    // LAYER 3 → LAYER 4: Calls alumniRepository.findByStudent_StudentId() to find the matching row
    public Optional<Alumni> getByStudentId(String studentId) {
        return alumniRepository.findByStudent_StudentId(studentId);

    }

    // LAYER 2 → LAYER 3: Called by AlumniController.graduate() when the registrar graduates a student
    // LAYER 3 → LAYER 4: Changes the student's status to Alumni, then creates an Alumni record — both in one transaction
    // @Transactional: if either save fails, both changes are rolled back to keep data consistent
    @Transactional
    public Alumni graduateStudent(String studentId, LocalDate graduationDate, String honors) {
        Student student = studentRepository.findByStudentId(studentId)
            .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        if (student.getCurrentStatus() == Student.StudentStatus.Alumni) {
            throw new RuntimeException("Student is already an alumnus.");
        }

        student.setCurrentStatus(Student.StudentStatus.Alumni);
        studentRepository.save(student);

        Alumni alumni = Alumni.builder()
            .alumniId("ALM-" + System.currentTimeMillis())
            .student(student)
            .program(student.getProgram())
            .graduationDate(graduationDate)
            .yearGraduated(String.valueOf(graduationDate.getYear()))
            .honors(honors)
            .build();
        return alumniRepository.save(alumni);
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.update() when the registrar edits alumni details
    // LAYER 3 → LAYER 4: Calls alumniRepository.save() to persist the updated Alumni object
    @Transactional
    public Alumni update(Alumni alumni) {
        return alumniRepository.save(alumni);
    }

    // LAYER 2 → LAYER 3: Called by AlumniController.unmarkAlumni() to reverse a graduation
    // LAYER 3 → LAYER 4: Deletes the Alumni record first (to release the FK constraint),
    //   then reactivates the student by setting their status back to Active — both in one transaction
    @Transactional
    public void unmarkAlumni(String alumniId) {
        Alumni alumni = alumniRepository.findByAlumniId(alumniId)
            .orElseThrow(() -> new RuntimeException("Alumni record not found: " + alumniId));
        // Capture student reference and PK before deleting alumni
        Student student = alumni.getStudent();
        Integer alumniPk = alumni.getIndex();
        // Delete alumni record first to release the FK constraint on student
        alumniRepository.deleteById(alumniPk);
        alumniRepository.flush();
        // Then revert student status
        if (student != null) {
            student.setCurrentStatus(Student.StudentStatus.Active);
            studentRepository.save(student);
        }
    }
}

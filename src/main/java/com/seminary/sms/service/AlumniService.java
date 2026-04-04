package com.seminary.sms.service;

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

    public List<Alumni> getAll() {
        return alumniRepository.findAll();
    }

    public Optional<Alumni> getByStudentId(String studentId) {
        return alumniRepository.findByStudent_StudentId(studentId);

    }

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

    @Transactional
    public Alumni update(Alumni alumni) {
        return alumniRepository.save(alumni);
    }
}

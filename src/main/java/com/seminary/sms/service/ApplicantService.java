package com.seminary.sms.service;

import com.seminary.sms.entity.*;
import com.seminary.sms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("null")
@Service
@RequiredArgsConstructor
public class ApplicantService {

    private final ApplicantRepository applicantRepository;
    private final ApplicationRepository applicationRepository;
    private final EntranceExamRepository entranceExamRepository;

    public List<Applicant> getAll() {
        return applicantRepository.findAll();
    }

    public Optional<Applicant> getById(String id) {
        return applicantRepository.findByApplicantId(id);
    }

    @Transactional
    public Applicant save(Applicant applicant) {
        return applicantRepository.save(applicant);
    }

    @Transactional
    public Application updateStatus(String applicationId, Application.ApplicationStatus status) {
        Application app = applicationRepository.findByApplicationId(applicationId)
            .orElseThrow(() -> new RuntimeException("Application not found: " + applicationId));
        app.setApplicationStatus(status);
        return applicationRepository.save(app);
    }

    @Transactional
    public EntranceExam recordExam(EntranceExam exam) {
        return entranceExamRepository.save(exam);
    }

    public List<Application> getApplicationsByStatus(Application.ApplicationStatus status) {
        return applicationRepository.findByApplicationStatus(status);
    }

    public Optional<Application> getApplicationByApplicant(String applicantId) {
        return applicationRepository.findByApplicant_ApplicantId(applicantId);
    }

    public List<EntranceExam> getExamsByApplicant(String applicantId) {
        return entranceExamRepository.findByApplicant_ApplicantId(applicantId);
    }
}

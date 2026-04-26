package com.seminary.sms.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendCredentials(String toEmail, String studentId, String tempPassword, String studentName) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("St. Francis de Sales Seminary — Your Student Account");
            message.setText(
                "Dear " + studentName + ",\n\n" +
                "You have been successfully enrolled at St. Francis de Sales Major Seminary.\n" +
                "Your student account has been created. Please use the credentials below to log in.\n\n" +
                "  Username : " + studentId + "\n" +
                "  Password : " + tempPassword + "\n\n" +
                "IMPORTANT: You are required to change your password immediately after your first login.\n\n" +
                "Do not share your credentials with anyone.\n\n" +
                "God bless,\n" +
                "St. Francis de Sales Major Seminary\n" +
                "Registrar's Office"
            );
            mailSender.send(message);
            log.info("Credentials email sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send credentials email to {}: {}", toEmail, e.getMessage());
        }
    }
}

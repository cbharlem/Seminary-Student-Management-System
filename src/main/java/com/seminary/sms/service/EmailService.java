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

    @Async
    public void sendAccountLocked(String toEmail, String username, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("St. Francis de Sales Seminary — Account Temporarily Locked");
            message.setText(
                "Dear " + username + ",\n\n" +
                "We detected multiple failed login attempts on your account. For your protection,\n" +
                "your account has been temporarily locked for 15 minutes.\n\n" +
                "If this was you, simply wait 15 minutes and try again.\n\n" +
                "If this was NOT you, someone may be trying to access your account.\n" +
                "You can bypass the lock and reset your password immediately by clicking the link below:\n\n" +
                "  " + resetLink + "\n\n" +
                "This link expires in 1 hour and can only be used once.\n\n" +
                "If you did not request this, you may ignore this email — your account will unlock automatically.\n\n" +
                "God bless,\n" +
                "St. Francis de Sales Major Seminary\n" +
                "Registrar's Office"
            );
            mailSender.send(message);
            log.info("Account locked notification sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send lock notification to {}: {}", toEmail, e.getMessage());
        }
    }

    @Async
    public void sendPasswordReset(String toEmail, String username, String resetLink) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("St. Francis de Sales Seminary — Password Reset Request");
            message.setText(
                "Dear " + username + ",\n\n" +
                "A password reset was requested for your account. Click the link below to set a new password:\n\n" +
                "  " + resetLink + "\n\n" +
                "This link expires in 1 hour and can only be used once.\n\n" +
                "If you did not request a password reset, you may safely ignore this email.\n\n" +
                "God bless,\n" +
                "St. Francis de Sales Major Seminary\n" +
                "Registrar's Office"
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}

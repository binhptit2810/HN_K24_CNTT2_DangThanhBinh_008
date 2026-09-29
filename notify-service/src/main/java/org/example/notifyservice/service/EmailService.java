package org.example.notifyservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${notification.mail.from:${spring.mail.username:}}") String from
    ) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendOrderCreatedEmail(String recipient) {
        if (!StringUtils.hasText(recipient)) {
            log.warn("Invalid recipient, skipping email sending");
            return;
        }
        String trimmedRecipient = recipient.trim();
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(from)) {
            message.setFrom(from);
        }
        message.setTo(trimmedRecipient);
        message.setSubject("Appointment Confirmation");
        message.setText("Your appointment has been successfully created.");

        mailSender.send(message);
        log.info("Sent appointment email to {}", trimmedRecipient);
    }
}
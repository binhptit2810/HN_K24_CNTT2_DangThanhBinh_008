package org.example.notifyservice.consumer;

import lombok.extern.slf4j.Slf4j;
import org.example.notifyservice.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AppointmentCreatedConsumer {
    private final EmailService emailService;

    public AppointmentCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(
            topics = "${notification.kafka.order-created-topic:appointment-created}",
            groupId = "${spring.kafka.consumer.group-id:notify-service}"
    )
    public void consume(String email) {
        if (!StringUtils.hasText(email)) {
            log.warn("Received empty or blank email payload, skipping email sending");
            return;
        }
        emailService.sendOrderCreatedEmail(email);
    }
}

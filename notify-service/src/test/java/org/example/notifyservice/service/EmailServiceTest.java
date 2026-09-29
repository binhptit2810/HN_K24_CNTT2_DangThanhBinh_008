package org.example.notifyservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    @DisplayName("sendOrderCreatedEmail trims recipient, sets from from config, and sends SimpleMailMessage")
    void sendOrderCreatedEmail_validRecipient_trimsAndSendsMail() {
        String configuredFrom = "noreply@hospital.com";
        EmailService emailService = new EmailService(mailSender, configuredFrom);

        String rawRecipient = "   patient@example.com   ";
        emailService.sendOrderCreatedEmail(rawRecipient);

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertThat(sentMessage).isNotNull();
        assertThat(sentMessage.getFrom()).isEqualTo("noreply@hospital.com");
        assertThat(Objects.requireNonNull(sentMessage.getTo())[0]).isEqualTo("patient@example.com");
        assertThat(sentMessage.getSubject()).isEqualTo("Appointment Confirmation");
        assertThat(sentMessage.getText()).isEqualTo("Your appointment has been successfully created.");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("sendOrderCreatedEmail skips sending when recipient is null, empty, or blank")
    void sendOrderCreatedEmail_invalidRecipient_skipsSending(String invalidRecipient) {
        EmailService emailService = new EmailService(mailSender, "noreply@hospital.com");

        emailService.sendOrderCreatedEmail(invalidRecipient);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}

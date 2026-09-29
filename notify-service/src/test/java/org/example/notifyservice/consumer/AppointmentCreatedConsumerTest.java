package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentCreatedConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentCreatedConsumer consumer;

    @Test
    @DisplayName("consume delegates to EmailService when email is valid")
    void consume_validEmail_callsEmailService() {
        String email = "patient@example.com";

        consumer.consume(email);

        verify(emailService, times(1)).sendOrderCreatedEmail(email);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("consume ignores null, empty, or blank email payloads")
    void consume_invalidEmail_skipsEmailSending(String invalidEmail) {
        consumer.consume(invalidEmail);

        verify(emailService, never()).sendOrderCreatedEmail(any());
    }
}

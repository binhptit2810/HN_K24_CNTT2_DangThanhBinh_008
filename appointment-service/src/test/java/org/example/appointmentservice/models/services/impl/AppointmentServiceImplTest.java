package org.example.appointmentservice.models.services.impl;

import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.models.constants.AppointmentStatus;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentDetailRequest;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentRequest;
import org.example.appointmentservice.models.dto.responses.AppointmentResponse;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.example.appointmentservice.models.entities.Appointment;
import org.example.appointmentservice.models.entities.AppointmentDetail;
import org.example.appointmentservice.models.repositories.AppointmentDetailRepository;
import org.example.appointmentservice.models.repositories.AppointmentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentDetailRepository appointmentDetailRepository;

    @Mock
    private DoctorGatewayService doctorGatewayService;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(appointmentService, "orderCreatedTopic", "appointment-created");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("createAppointment calculates lineTotal, totalAmount, sets PENDING, and saves appointment details")
    void createAppointment_successful() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(
                        new CreateAppointmentDetailRequest(1L, 2),
                        new CreateAppointmentDetailRequest(2L, 1)
                )
        );

        when(doctorGatewayService.getDoctorById(1L))
                .thenReturn(new DoctorResponse(1L, "Dr. Alice", "Cardiology", 100.0, 10));
        when(doctorGatewayService.getDoctorById(2L))
                .thenReturn(new DoctorResponse(2L, "Dr. Bob", "Dermatology", 150.0, 5));

        Appointment savedAppointment = Appointment.builder()
                .id(10L)
                .patientName("Nguyen Van A")
                .patientEmail("patient@example.com")
                .totalAmount(350.0)
                .status(AppointmentStatus.PENDING)
                .build();
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(savedAppointment);

        when(appointmentDetailRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<AppointmentDetail> details = invocation.getArgument(0);
            long idCounter = 100L;
            for (AppointmentDetail detail : details) {
                detail.setId(idCounter++);
            }
            return details;
        });

        AppointmentResponse response = appointmentService.createAppointment(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.patientName()).isEqualTo("Nguyen Van A");
        assertThat(response.patientEmail()).isEqualTo("patient@example.com");
        assertThat(response.totalAmount()).isEqualTo(350.0);
        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING);
        assertThat(response.details()).hasSize(2);

        assertThat(response.details().get(0).doctorId()).isEqualTo(1L);
        assertThat(response.details().get(0).slots()).isEqualTo(2);
        assertThat(response.details().get(0).consultationFee()).isEqualTo(100.0);
        assertThat(response.details().get(0).lineTotal()).isEqualTo(200.0);

        assertThat(response.details().get(1).doctorId()).isEqualTo(2L);
        assertThat(response.details().get(1).slots()).isEqualTo(1);
        assertThat(response.details().get(1).consultationFee()).isEqualTo(150.0);
        assertThat(response.details().get(1).lineTotal()).isEqualTo(150.0);

        // When no transaction is active, kafka is sent immediately
        verify(kafkaTemplate, times(1)).send("appointment-created", "patient@example.com");
    }

    @Test
    @DisplayName("createAppointment throws DoctorNotFoundException and does not persist appointment if a doctor is not found")
    void createAppointment_doctorNotFound_noPersistence() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(
                        new CreateAppointmentDetailRequest(1L, 2),
                        new CreateAppointmentDetailRequest(999L, 1)
                )
        );

        when(doctorGatewayService.getDoctorById(1L))
                .thenReturn(new DoctorResponse(1L, "Dr. Alice", "Cardiology", 100.0, 10));
        when(doctorGatewayService.getDoctorById(999L))
                .thenThrow(new DoctorNotFoundException(999L));

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("999");

        verify(appointmentRepository, never()).save(any());
        verify(appointmentDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }

    @Test
    @DisplayName("createAppointment throws DoctorServiceException and does not persist appointment on service failure")
    void createAppointment_doctorServiceError_noPersistence() {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(new CreateAppointmentDetailRequest(1L, 2))
        );

        when(doctorGatewayService.getDoctorById(1L))
                .thenThrow(new DoctorServiceException("Doctor service unavailable"));

        assertThatThrownBy(() -> appointmentService.createAppointment(request))
                .isInstanceOf(DoctorServiceException.class)
                .hasMessageContaining("Doctor service unavailable");

        verify(appointmentRepository, never()).save(any());
        verify(appointmentDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(any(), any());
    }


}

package org.example.appointmentservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.exceptions.GlobalExceptionHandler;
import org.example.appointmentservice.models.constants.AppointmentStatus;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentDetailRequest;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentRequest;
import org.example.appointmentservice.models.dto.responses.AppointmentResponse;
import org.example.appointmentservice.models.services.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerValidationTest {

    private MockMvc mockMvc;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(appointmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Valid request returns 201 CREATED")
    void createAppointment_valid_returns201() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(new CreateAppointmentDetailRequest(1L, 2))
        );

        AppointmentResponse response = new AppointmentResponse(
                1L,
                "Nguyen Van A",
                "patient@example.com",
                200.0,
                AppointmentStatus.PENDING,
                Collections.emptyList()
        );

        when(appointmentService.createAppointment(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.patientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("Blank patientName returns 400 BAD_REQUEST")
    void createAppointment_blankPatientName_returns400() throws Exception {
        String body = """
                {
                    "patientName": "   ",
                    "patientEmail": "patient@example.com",
                    "items": [{"doctorId": 1, "slots": 1}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Blank patientEmail returns 400 BAD_REQUEST")
    void createAppointment_blankPatientEmail_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "   ",
                    "items": [{"doctorId": 1, "slots": 1}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Invalid patientEmail format returns 400 BAD_REQUEST")
    void createAppointment_invalidPatientEmail_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "not-an-email",
                    "items": [{"doctorId": 1, "slots": 1}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Empty items list returns 400 BAD_REQUEST")
    void createAppointment_emptyItems_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "patient@example.com",
                    "items": []
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Null doctorId returns 400 BAD_REQUEST")
    void createAppointment_nullDoctorId_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "patient@example.com",
                    "items": [{"doctorId": null, "slots": 1}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Null slots returns 400 BAD_REQUEST")
    void createAppointment_nullSlots_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "patient@example.com",
                    "items": [{"doctorId": 1, "slots": null}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("Non-positive slots returns 400 BAD_REQUEST")
    void createAppointment_nonPositiveSlots_returns400() throws Exception {
        String body = """
                {
                    "patientName": "Nguyen Van A",
                    "patientEmail": "patient@example.com",
                    "items": [{"doctorId": 1, "slots": 0}]
                }
                """;

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"));
    }

    @Test
    @DisplayName("DoctorNotFoundException translates to 404 NOT_FOUND")
    void createAppointment_doctorNotFound_returns404() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(new CreateAppointmentDetailRequest(999L, 1))
        );

        when(appointmentService.createAppointment(any())).thenThrow(new DoctorNotFoundException(999L));

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Doctor not found"));
    }

    @Test
    @DisplayName("DoctorServiceException translates to 502 BAD_GATEWAY")
    void createAppointment_doctorServiceError_returns502() throws Exception {
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                "Nguyen Van A",
                "patient@example.com",
                List.of(new CreateAppointmentDetailRequest(1L, 1))
        );

        when(appointmentService.createAppointment(any())).thenThrow(new DoctorServiceException("Downstream error"));

        mockMvc.perform(post("/api/v1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Doctor service error"));
    }
}

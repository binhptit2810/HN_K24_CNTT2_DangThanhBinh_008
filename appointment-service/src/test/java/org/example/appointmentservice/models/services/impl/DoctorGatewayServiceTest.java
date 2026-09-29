package org.example.appointmentservice.models.services.impl;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.example.appointmentservice.clients.DoctorClient;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorGatewayServiceTest {

    @Mock
    private DoctorClient doctorClient;

    @InjectMocks
    private DoctorGatewayService doctorGatewayService;

    private Request createDummyRequest() {
        return Request.create(
                Request.HttpMethod.GET,
                "/api/v1/doctors/1",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );
    }

    @Test
    @DisplayName("getDoctorById returns DoctorResponse when doctor-service responds successfully")
    void getDoctorById_success() {
        DoctorResponse expected = new DoctorResponse(1L, "Dr. Alice", "Neurology", 200.0, 5);
        when(doctorClient.getDoctorById(1L)).thenReturn(expected);

        DoctorResponse actual = doctorGatewayService.getDoctorById(1L);

        assertThat(actual).isEqualTo(expected);
        verify(doctorClient, times(1)).getDoctorById(1L);
    }

    @Test
    @DisplayName("getDoctorById translates Feign 404 to DoctorNotFoundException")
    void getDoctorById_notFound_throwsDoctorNotFoundException() {
        FeignException.NotFound notFound = new FeignException.NotFound(
                "Not Found",
                createDummyRequest(),
                new byte[0],
                Collections.emptyMap()
        );
        when(doctorClient.getDoctorById(99L)).thenThrow(notFound);

        assertThatThrownBy(() -> doctorGatewayService.getDoctorById(99L))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getDoctorById translates system error to DoctorServiceException")
    void getDoctorById_systemError_throwsDoctorServiceException() {
        when(doctorClient.getDoctorById(1L)).thenThrow(new RuntimeException("Connection timeout"));

        assertThatThrownBy(() -> doctorGatewayService.getDoctorById(1L))
                .isInstanceOf(DoctorServiceException.class)
                .hasMessageContaining("Connection timeout");
    }

    @Test
    @DisplayName("getDoctorByIdFallback rethrows DoctorNotFoundException without faking doctor")
    void fallback_rethrowsDoctorNotFoundException() {
        DoctorNotFoundException notFound = new DoctorNotFoundException(99L);

        assertThatThrownBy(() -> doctorGatewayService.getDoctorByIdFallback(99L, notFound))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("getDoctorByIdFallback wraps general failure into DoctorServiceException without faking doctor")
    void fallback_wrapsIntoDoctorServiceException() {
        RuntimeException timeout = new RuntimeException("Circuit breaker open");

        assertThatThrownBy(() -> doctorGatewayService.getDoctorByIdFallback(1L, timeout))
                .isInstanceOf(DoctorServiceException.class)
                .hasMessageContaining("Circuit breaker open");
    }
}

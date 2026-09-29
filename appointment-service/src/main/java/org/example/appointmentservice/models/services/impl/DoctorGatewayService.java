package org.example.appointmentservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointmentservice.clients.DoctorClient;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.exceptions.DoctorServiceException;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorGatewayService {
    private final DoctorClient doctorClient;

    @CircuitBreaker(name = "appointmentService", fallbackMethod = "getDoctorByIdFallback")
    public DoctorResponse getDoctorById(Long doctorId) {
        try {
            return doctorClient.getDoctorById(doctorId);
        } catch (FeignException.NotFound e) {
            throw new DoctorNotFoundException(doctorId);
        } catch (FeignException e) {
            if (e.status() == 404) {
                throw new DoctorNotFoundException(doctorId);
            }
            throw new DoctorServiceException("Doctor service error: " + e.getMessage(), e);
        } catch (DoctorNotFoundException | DoctorServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DoctorServiceException("Doctor service error: " + e.getMessage(), e);
        }
    }

    public DoctorResponse getDoctorByIdFallback(Long doctorId, Throwable throwable) {
        if (throwable instanceof DoctorNotFoundException notFound) {
            throw notFound;
        }
        if (throwable instanceof FeignException fe && fe.status() == 404) {
            throw new DoctorNotFoundException(doctorId);
        }
        if (throwable instanceof DoctorServiceException dse) {
            throw dse;
        }
        throw new DoctorServiceException("Doctor service is unavailable: " + throwable.getMessage(), throwable);
    }
}

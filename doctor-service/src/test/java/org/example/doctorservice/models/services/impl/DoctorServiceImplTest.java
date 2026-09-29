package org.example.doctorservice.models.services.impl;

import org.example.doctorservice.exceptions.DoctorNotFoundException;
import org.example.doctorservice.models.entities.Doctor;
import org.example.doctorservice.models.repositories.DoctorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private Doctor sampleDoctor;

    @BeforeEach
    void setUp() {
        sampleDoctor = Doctor.builder()
                .id(1L)
                .doctorName("Dr. John Doe")
                .specialization("Cardiology")
                .consultationFee(150.0)
                .availableSlots(10)
                .build();
    }

    @Test
    @DisplayName("getDoctorById returns existing doctor successfully")
    void getDoctorById_whenDoctorExists_returnsDoctor() {
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));

        Doctor result = doctorService.getDoctorById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDoctorName()).isEqualTo("Dr. John Doe");
        assertThat(result.getSpecialization()).isEqualTo("Cardiology");
        assertThat(result.getConsultationFee()).isEqualTo(150.0);
        assertThat(result.getAvailableSlots()).isEqualTo(10);
        verify(doctorRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("getDoctorById throws DoctorNotFoundException when doctor does not exist")
    void getDoctorById_whenDoctorNotFound_throwsDoctorNotFoundException() {
        when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> doctorService.getDoctorById(99L))
                .isInstanceOf(DoctorNotFoundException.class)
                .hasMessageContaining("99");

        verify(doctorRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("verify @Cacheable contract: cacheNames='doctors', key='#id', sync=true")
    void verifyCacheableAnnotationContract() throws NoSuchMethodException {
        Method method = DoctorServiceImpl.class.getMethod("getDoctorById", Long.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly("doctors");
        assertThat(cacheable.key()).isEqualTo("#id");
        assertThat(cacheable.sync()).isTrue();
    }
}

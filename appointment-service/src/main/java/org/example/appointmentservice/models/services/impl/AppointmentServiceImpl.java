package org.example.appointmentservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.appointmentservice.exceptions.DoctorNotFoundException;
import org.example.appointmentservice.models.constants.AppointmentStatus;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentDetailRequest;
import org.example.appointmentservice.models.dto.requests.CreateAppointmentRequest;
import org.example.appointmentservice.models.dto.responses.AppointmentDetailResponse;
import org.example.appointmentservice.models.dto.responses.AppointmentResponse;
import org.example.appointmentservice.models.dto.responses.DoctorResponse;
import org.example.appointmentservice.models.entities.Appointment;
import org.example.appointmentservice.models.entities.AppointmentDetail;
import org.example.appointmentservice.models.repositories.AppointmentDetailRepository;
import org.example.appointmentservice.models.repositories.AppointmentRepository;
import org.example.appointmentservice.models.services.AppointmentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentDetailRepository appointmentDetailRepository;
    private final DoctorGatewayService doctorGatewayService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${notification.kafka.order-created-topic:${ORDER_CREATED_TOPIC:appointment-created}}")
    private String orderCreatedTopic;

    @Override
    @Transactional
    public AppointmentResponse createAppointment(CreateAppointmentRequest request) {
        List<AppointmentDetail> details = new ArrayList<>();
        double totalAmount = 0.0;

        for (CreateAppointmentDetailRequest item : request.items()) {
            DoctorResponse doctor = doctorGatewayService.getDoctorById(item.doctorId());
            if (doctor == null) {
                throw new DoctorNotFoundException(item.doctorId());
            }
            double fee = doctor.consultationFee() != null ? doctor.consultationFee() : 0.0;
            double lineTotal = fee * item.slots();
            totalAmount += lineTotal;

            AppointmentDetail detail = AppointmentDetail.builder()
                    .doctorId(item.doctorId())
                    .slots(item.slots())
                    .consultationFee(fee)
                    .lineTotal(lineTotal)
                    .build();
            details.add(detail);
        }

        Appointment appointment = Appointment.builder()
                .patientName(request.patientName())
                .patientEmail(request.patientEmail())
                .totalAmount(totalAmount)
                .status(AppointmentStatus.PENDING)
                .build();
        Appointment savedAppointment = appointmentRepository.save(appointment);

        for (AppointmentDetail detail : details) {
            detail.setAppointment(savedAppointment);
        }
        List<AppointmentDetail> savedDetails = appointmentDetailRepository.saveAll(details);

        String patientEmail = savedAppointment.getPatientEmail();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAppointmentCreatedEvent(patientEmail);
                }
            });
        } else {
            sendAppointmentCreatedEvent(patientEmail);
        }

        List<AppointmentDetailResponse> detailResponses = savedDetails.stream()
                .map(d -> new AppointmentDetailResponse(
                        d.getId(),
                        d.getDoctorId(),
                        d.getSlots(),
                        d.getConsultationFee(),
                        d.getLineTotal()
                ))
                .toList();

        return new AppointmentResponse(
                savedAppointment.getId(),
                savedAppointment.getPatientName(),
                savedAppointment.getPatientEmail(),
                savedAppointment.getTotalAmount(),
                savedAppointment.getStatus(),
                detailResponses
        );
    }

    private void sendAppointmentCreatedEvent(String patientEmail) {
        try {
            kafkaTemplate.send(orderCreatedTopic, patientEmail);
            log.info("Dispatched appointment-created event for email: {}", patientEmail);
        } catch (Exception e) {
            log.error("Failed to dispatch appointment-created event to Kafka: {}", e.getMessage(), e);
        }
    }
}

package cl.duoc.barriodigital.requests.messaging.producer;

import cl.duoc.barriodigital.requests.config.RabbitMQConstants;
import cl.duoc.barriodigital.requests.messaging.event.CertificateGenerationEvent;
import cl.duoc.barriodigital.requests.messaging.event.CrewTicketEvent;
import cl.duoc.barriodigital.requests.messaging.event.EmailNotificationEvent;
import cl.duoc.barriodigital.requests.messaging.event.EventEnvelope;
import cl.duoc.barriodigital.requests.model.Request;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Implementación de producción para publicación de eventos asíncronos en RabbitMQ.
 * Usa nombres de colas, exchanges y routing keys centralizados en RabbitMQConstants.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitRequestEventProducer implements RequestEventProducer {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publishEmailNotification(String eventType, Request request, String messageText) {
        try {
            EmailNotificationEvent payload = EmailNotificationEvent.builder()
                    .trackingNumber(request.getTrackingNumber())
                    .requestId(request.getId())
                    .recipientEmail(request.getCitizenEmail())
                    .recipientName(request.getCitizenName())
                    .subject("BarrioDigital - Actualización trámite: " + request.getTrackingNumber())
                    .message(messageText)
                    .eventType(eventType)
                    .newStatus(request.getStatus() != null ? request.getStatus().name() : null)
                    .build();

            EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.of(
                    "EMAIL_NOTIFICATION",
                    request.getTrackingNumber(),
                    payload
            );

            // Publicación a direct exchange
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT,
                    RabbitMQConstants.ROUTING_KEY_EMAIL_SEND,
                    envelope
            );
            log.info("Mensaje publicado en RabbitMQ [Exchange: {}, RoutingKey: {}] para solicitud: {}",
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_EMAIL_SEND, request.getTrackingNumber());
        } catch (Exception e) {
            log.warn("No fue posible enviar mensaje RabbitMQ de Email ({}), continuando ejecución: {}",
                    request.getTrackingNumber(), e.getMessage());
        }
    }

    @Override
    public void publishCrewTicket(Request request, String assignedCrew) {
        try {
            CrewTicketEvent payload = CrewTicketEvent.builder()
                    .trackingNumber(request.getTrackingNumber())
                    .requestId(request.getId())
                    .procedureName(request.getProcedureName())
                    .citizenName(request.getCitizenName())
                    .citizenPhone(request.getCitizenPhone())
                    .address(request.getAddress())
                    .description(request.getDescription())
                    .assignedCrew(assignedCrew)
                    .scheduledBy(request.getVisitedBy())
                    .scheduledAt(LocalDateTime.now())
                    .build();

            EventEnvelope<CrewTicketEvent> envelope = EventEnvelope.of(
                    "CREW_TICKET",
                    request.getTrackingNumber(),
                    payload
            );

            // Publicación a direct exchange
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT,
                    RabbitMQConstants.ROUTING_KEY_CREW_TICKET,
                    envelope
            );
            log.info("Ticket cuadrilla publicado en RabbitMQ [Exchange: {}, RoutingKey: {}] para solicitud: {}",
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CREW_TICKET, request.getTrackingNumber());
        } catch (Exception e) {
            log.warn("No fue posible enviar mensaje RabbitMQ de Cuadrilla ({}), continuando ejecución: {}",
                    request.getTrackingNumber(), e.getMessage());
        }
    }

    @Override
    public void publishCertificateGeneration(String documentType, Request request) {
        try {
            CertificateGenerationEvent payload = CertificateGenerationEvent.builder()
                    .trackingNumber(request.getTrackingNumber())
                    .requestId(request.getId())
                    .documentType(documentType)
                    .citizenDni(request.getCitizenDni())
                    .citizenName(request.getCitizenName())
                    .procedureName(request.getProcedureName())
                    .status(request.getStatus() != null ? request.getStatus().name() : null)
                    .resolutionNotes(request.getResolutionNotes())
                    .issuedAt(LocalDateTime.now())
                    .build();

            EventEnvelope<CertificateGenerationEvent> envelope = EventEnvelope.of(
                    "CERTIFICATE_GENERATION",
                    request.getTrackingNumber(),
                    payload
            );

            // Publicación a direct exchange
            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT,
                    RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GEN,
                    envelope
            );
            log.info("Comprobante/Certificado publicado en RabbitMQ [Exchange: {}, RoutingKey: {}] para solicitud: {}",
                    RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GEN, request.getTrackingNumber());
        } catch (Exception e) {
            log.warn("No fue posible enviar mensaje RabbitMQ de Certificado ({}), continuando ejecución: {}",
                    request.getTrackingNumber(), e.getMessage());
        }
    }
}

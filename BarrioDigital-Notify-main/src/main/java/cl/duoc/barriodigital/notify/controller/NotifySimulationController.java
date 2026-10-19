package cl.duoc.barriodigital.notify.controller;

import cl.duoc.barriodigital.notify.config.RabbitMQConstants;
import cl.duoc.barriodigital.notify.model.CertificateGenerationEvent;
import cl.duoc.barriodigital.notify.model.CrewTicketEvent;
import cl.duoc.barriodigital.notify.model.EmailNotificationEvent;
import cl.duoc.barriodigital.notify.model.EventEnvelope;
import cl.duoc.barriodigital.notify.service.CertificateService;
import cl.duoc.barriodigital.notify.service.CrewTicketService;
import cl.duoc.barriodigital.notify.service.DeadLetterAuditService;
import cl.duoc.barriodigital.notify.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/notify")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NotifySimulationController {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationService notificationService;
    private final CrewTicketService crewTicketService;
    private final CertificateService certificateService;
    private final DeadLetterAuditService deadLetterAuditService;

    @PostMapping("/simulate/email")
    public ResponseEntity<?> simulateEmail(@RequestParam(defaultValue = "vecino@barriodigital.cl") String email) {
        EmailNotificationEvent payload = EmailNotificationEvent.builder()
                .trackingNumber("BD-2025-" + (int)(Math.random() * 9000 + 1000))
                .requestId(100L)
                .recipientEmail(email)
                .recipientName("Vecino Prueba")
                .subject("Prueba de Notificación BarrioDigital")
                .message("Este es un mensaje de prueba emitido hacia RabbitMQ direct exchange")
                .eventType("TEST_EMAIL")
                .newStatus("ADMITIDO")
                .build();

        EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.<EmailNotificationEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("EMAIL_NOTIFICATION")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_EMAIL_SEND, envelope);
        return ResponseEntity.ok(Map.of("message", "Mensaje enviado a cola de email", "tracking", payload.getTrackingNumber()));
    }

    @PostMapping("/simulate/poison-pill")
    public ResponseEntity<?> simulatePoisonPill() {
        // Enviar un mensaje diseñado para fallar en el consumidor y verificar el desvío a DLQ
        EmailNotificationEvent payload = EmailNotificationEvent.builder()
                .trackingNumber("BD-FAIL-9999")
                .recipientEmail("poison.error@simulacion.cl")
                .recipientName("Test Fallo")
                .subject("Mensaje envenenado para prueba de DLQ")
                .message("Este mensaje generará excepción en consumidor y se enviará a q.cmd.email.dlq")
                .eventType("SIMULATED_FAILURE")
                .build();

        EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.<EmailNotificationEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("EMAIL_NOTIFICATION")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_EMAIL_SEND, envelope);
        return ResponseEntity.ok(Map.of(
                "message", "Mensaje de fallo simulado enviado. El consumidor rechazará con NACK sin requeue y RabbitMQ lo desviará a la DLQ q.cmd.email.dlq",
                "tracking", payload.getTrackingNumber()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory() {
        return ResponseEntity.ok(Map.of(
                "deliveredEmails", notificationService.getDeliveredNotifications(),
                "dispatchedTickets", crewTicketService.getDispatchedTickets(),
                "generatedCertificates", certificateService.getGeneratedCertificates(),
                "deadLetterRecords", deadLetterAuditService.getDeadLetterRecords()
        ));
    }
}

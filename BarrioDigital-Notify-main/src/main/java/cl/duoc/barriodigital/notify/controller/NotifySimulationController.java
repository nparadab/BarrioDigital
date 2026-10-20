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
        EmailNotificationEvent payload = EmailNotificationEvent.builder()
                .trackingNumber("BD-FAIL-EMAIL-" + (int)(Math.random() * 9000 + 1000))
                .recipientEmail("poison.error@simulacion.cl")
                .recipientName("Test Fallo Email")
                .subject("Mensaje envenenado para prueba de DLQ de Email")
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
                "queue", RabbitMQConstants.QUEUE_CMD_EMAIL,
                "dlq", RabbitMQConstants.QUEUE_CMD_EMAIL_DLQ,
                "message", "Mensaje de fallo simulado enviado. El consumidor rechazará con NACK (requeue=false) y RabbitMQ lo desviará a q.cmd.email.dlq",
                "tracking", payload.getTrackingNumber()
        ));
    }

    @PostMapping("/simulate/crew")
    public ResponseEntity<?> simulateCrew(@RequestParam(defaultValue = "Cuadrilla Centro 01") String crew) {
        CrewTicketEvent payload = CrewTicketEvent.builder()
                .trackingNumber("BD-CREW-" + (int)(Math.random() * 9000 + 1000))
                .requestId(200L)
                .procedureName("Reparación Luminaria")
                .citizenName("Vecino Cuadrilla")
                .citizenPhone("+56912345678")
                .address("Av. Principal 1234")
                .description("Foco apagado en vía pública")
                .assignedCrew(crew)
                .scheduledBy("funcionario@barriodigital.cl")
                .scheduledAt(LocalDateTime.now())
                .build();

        EventEnvelope<CrewTicketEvent> envelope = EventEnvelope.<CrewTicketEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("CREW_TICKET")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CREW_TICKET, envelope);
        return ResponseEntity.ok(Map.of("message", "Ticket de cuadrilla enviado a q.cmd.crew vía cmd.direct", "tracking", payload.getTrackingNumber()));
    }

    @PostMapping("/simulate/crew/poison-pill")
    public ResponseEntity<?> simulateCrewPoisonPill() {
        CrewTicketEvent payload = CrewTicketEvent.builder()
                .trackingNumber("BD-FAIL-CREW-" + (int)(Math.random() * 9000 + 1000))
                .requestId(201L)
                .procedureName("Falla Cuadrilla Test")
                .address("Calle Error 404")
                .assignedCrew("error-cuadrilla-poison")
                .scheduledAt(LocalDateTime.now())
                .build();

        EventEnvelope<CrewTicketEvent> envelope = EventEnvelope.<CrewTicketEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("CREW_TICKET")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CREW_TICKET, envelope);
        return ResponseEntity.ok(Map.of(
                "queue", RabbitMQConstants.QUEUE_CMD_CREW,
                "dlq", RabbitMQConstants.QUEUE_CMD_CREW_DLQ,
                "message", "Fallo de cuadrilla simulado enviado. El consumidor rechazará con NACK (requeue=false) y RabbitMQ lo desviará a q.cmd.crew.dlq",
                "tracking", payload.getTrackingNumber()
        ));
    }

    @PostMapping("/simulate/certificate")
    public ResponseEntity<?> simulateCertificate(@RequestParam(defaultValue = "CERTIFICADO_RESIDENCIA") String docType) {
        CertificateGenerationEvent payload = CertificateGenerationEvent.builder()
                .trackingNumber("BD-CERT-" + (int)(Math.random() * 9000 + 1000))
                .requestId(300L)
                .documentType(docType)
                .citizenDni("12345678-9")
                .citizenName("Vecino Certificado")
                .procedureName("Certificado de Residencia")
                .status("COMPLETADO")
                .resolutionNotes("Emitido electrónicamente")
                .issuedAt(LocalDateTime.now())
                .build();

        EventEnvelope<CertificateGenerationEvent> envelope = EventEnvelope.<CertificateGenerationEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("CERTIFICATE_GENERATION")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GEN, envelope);
        return ResponseEntity.ok(Map.of("message", "Generación de certificado enviada a q.cmd.certificate vía cmd.direct", "tracking", payload.getTrackingNumber()));
    }

    @PostMapping("/simulate/certificate/poison-pill")
    public ResponseEntity<?> simulateCertificatePoisonPill() {
        CertificateGenerationEvent payload = CertificateGenerationEvent.builder()
                .trackingNumber("BD-FAIL-CERT-" + (int)(Math.random() * 9000 + 1000))
                .requestId(301L)
                .documentType("error-poison-doc")
                .citizenDni("99999999-9")
                .citizenName("Vecino Error Doc")
                .issuedAt(LocalDateTime.now())
                .build();

        EventEnvelope<CertificateGenerationEvent> envelope = EventEnvelope.<CertificateGenerationEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("CERTIFICATE_GENERATION")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_DIRECT, RabbitMQConstants.ROUTING_KEY_CERTIFICATE_GEN, envelope);
        return ResponseEntity.ok(Map.of(
                "queue", RabbitMQConstants.QUEUE_CMD_CERTIFICATE,
                "dlq", RabbitMQConstants.QUEUE_CMD_CERTIFICATE_DLQ,
                "message", "Fallo de certificado simulado enviado. El consumidor rechazará con NACK (requeue=false) y RabbitMQ lo desviará a q.cmd.certificate.dlq",
                "tracking", payload.getTrackingNumber()
        ));
    }

    @PostMapping("/simulate/topic")
    public ResponseEntity<?> simulateTopic(
            @RequestParam(defaultValue = "email.alerta") String routingKey,
            @RequestParam(defaultValue = "Mensaje broadcast vía topic exchange") String message
    ) {
        EmailNotificationEvent payload = EmailNotificationEvent.builder()
                .trackingNumber("BD-TOPIC-" + (int)(Math.random() * 9000 + 1000))
                .recipientEmail("vecino.comunidad@barriodigital.cl")
                .recipientName("Comunidad Vecinal")
                .subject("Alerta Comunitaria (" + routingKey + ")")
                .message(message)
                .eventType("TOPIC_EVENT")
                .build();

        EventEnvelope<EmailNotificationEvent> envelope = EventEnvelope.<EmailNotificationEvent>builder()
                .eventId(UUID.randomUUID().toString())
                .type("TOPIC_MESSAGE")
                .timestamp(LocalDateTime.now())
                .correlationId(payload.getTrackingNumber())
                .payload(payload)
                .build();

        rabbitTemplate.convertAndSend(RabbitMQConstants.EXCHANGE_CMD_TOPIC, routingKey, envelope);
        return ResponseEntity.ok(Map.of(
                "exchange", RabbitMQConstants.EXCHANGE_CMD_TOPIC,
                "routingKey", routingKey,
                "message", "Mensaje enrutado a través del Topic Exchange cmd.topic",
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

package cl.duoc.barriodigital.notify.service;

import cl.duoc.barriodigital.notify.model.EmailNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de negocio para despacho de notificaciones (Email / Push).
 * Totalmente desacoplado de la infraestructura de mensajería.
 */
@Service
@Slf4j
public class NotificationService {

    private final List<Map<String, Object>> deliveredNotifications = Collections.synchronizedList(new ArrayList<>());

    public void processEmailNotification(EmailNotificationEvent event) {
        log.info("[NOTIFICACION-CORE] Procesando despacho de email para solicitud {} (Destinatario: {})",
                event.getTrackingNumber(), event.getRecipientEmail());

        // Validación de negocio: Simulación de fallo para comprobación de DLQ
        if (event.getRecipientEmail() == null || event.getRecipientEmail().isBlank()) {
            throw new IllegalArgumentException("Error de negocio no recuperable: El correo del destinatario es obligatorio");
        }
        if (event.getRecipientEmail().contains("error") || event.getRecipientEmail().contains("fail") || event.getRecipientEmail().contains("poison")) {
            throw new IllegalStateException("Fallo provocado intencionalmente para prueba de DLQ en correo: " + event.getRecipientEmail());
        }

        // Simulación exitosa de envío SMTP / Push
        Map<String, Object> record = new ConcurrentHashMap<>();
        record.put("trackingNumber", event.getTrackingNumber() != null ? event.getTrackingNumber() : "N/A");
        record.put("recipient", event.getRecipientEmail());
        record.put("subject", event.getSubject() != null ? event.getSubject() : "Notificación");
        record.put("eventType", event.getEventType() != null ? event.getEventType() : "INFO");
        record.put("status", "ENTREGADO");
        record.put("timestamp", LocalDateTime.now().toString());

        deliveredNotifications.add(record);
        log.info("[NOTIFICACION-CORE] Email despachado exitosamente a '{}' para trámite '{}'",
                event.getRecipientEmail(), event.getTrackingNumber());
    }

    public List<Map<String, Object>> getDeliveredNotifications() {
        return new ArrayList<>(deliveredNotifications);
    }
}

package cl.duoc.barriodigital.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de auditoría y monitoreo de mensajes no entregados que caen en las Dead Letter Queues (DLQ).
 * Cumple con el requisito: "Los microservicios deben manejar mensajes no entregados enviándolos a DLQ y registrándolos en logs."
 */
@Service
@Slf4j
public class DeadLetterAuditService {

    private final List<Map<String, Object>> deadLetterRecords = Collections.synchronizedList(new ArrayList<>());

    public void logDeadLetter(String queueName, String payload, String reason, long deliveryTag) {
        log.error("[DLQ-AUDIT] ¡ALERTA! Mensaje no entregado recibido en Dead Letter Queue '{}'. DeliveryTag: {}. Causa: {}. Contenido: {}",
                queueName, deliveryTag, reason, payload);

        Map<String, Object> record = new ConcurrentHashMap<>();
        record.put("dlq", queueName);
        record.put("deliveryTag", deliveryTag);
        record.put("reason", reason);
        record.put("payload", payload);
        record.put("loggedAt", LocalDateTime.now().toString());

        deadLetterRecords.add(record);
    }

    public List<Map<String, Object>> getDeadLetterRecords() {
        return new ArrayList<>(deadLetterRecords);
    }
}

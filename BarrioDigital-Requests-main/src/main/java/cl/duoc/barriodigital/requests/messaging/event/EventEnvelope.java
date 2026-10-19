package cl.duoc.barriodigital.requests.messaging.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Envelope común estándar para mensajería y streaming.
 * Incluye metadatos de trazabilidad: type, eventId, timestamp, traceId, correlationId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEnvelope<T> implements Serializable {

    private String eventId;
    private String type;
    private LocalDateTime timestamp;
    private String traceId;
    private String correlationId;
    private T payload;

    public static <T> EventEnvelope<T> of(String type, String correlationId, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .type(type)
                .timestamp(LocalDateTime.now())
                .traceId(UUID.randomUUID().toString())
                .correlationId(correlationId)
                .payload(payload)
                .build();
    }
}

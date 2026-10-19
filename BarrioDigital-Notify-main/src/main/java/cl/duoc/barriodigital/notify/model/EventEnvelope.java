package cl.duoc.barriodigital.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

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
}

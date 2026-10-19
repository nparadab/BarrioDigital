package cl.duoc.barriodigital.notify.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationEvent implements Serializable {
    private String trackingNumber;
    private Long requestId;
    private String recipientEmail;
    private String recipientName;
    private String subject;
    private String message;
    private String eventType;
    private String newStatus;
}

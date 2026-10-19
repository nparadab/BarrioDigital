package cl.duoc.barriodigital.requests.messaging.event;

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
public class CrewTicketEvent implements Serializable {
    private String trackingNumber;
    private Long requestId;
    private String procedureName;
    private String citizenName;
    private String citizenPhone;
    private String address;
    private String description;
    private String assignedCrew;
    private String scheduledBy;
    private LocalDateTime scheduledAt;
}

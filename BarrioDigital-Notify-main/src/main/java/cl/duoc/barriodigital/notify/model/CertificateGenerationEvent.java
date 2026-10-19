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
public class CertificateGenerationEvent implements Serializable {
    private String trackingNumber;
    private Long requestId;
    private String documentType;
    private String citizenDni;
    private String citizenName;
    private String procedureName;
    private String status;
    private String resolutionNotes;
    private LocalDateTime issuedAt;
}

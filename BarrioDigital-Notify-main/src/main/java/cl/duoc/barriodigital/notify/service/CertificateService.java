package cl.duoc.barriodigital.notify.service;

import cl.duoc.barriodigital.notify.model.CertificateGenerationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de negocio para generación de comprobantes de ingreso y certificados en formato PDF.
 */
@Service
@Slf4j
public class CertificateService {

    private final List<Map<String, Object>> generatedCertificates = Collections.synchronizedList(new ArrayList<>());

    public void processCertificateGeneration(CertificateGenerationEvent event) {
        log.info("[DOCUMENTOS-CORE] Generando documento tipo '{}' para trámite '{}'",
                event.getDocumentType(), event.getTrackingNumber());

        if (event.getDocumentType() == null || event.getDocumentType().isBlank()) {
            throw new IllegalArgumentException("Error no recuperable: Tipo de documento no especificado");
        }
        if (event.getDocumentType().contains("error") || event.getDocumentType().contains("poison")) {
            throw new IllegalStateException("Fallo provocado intencionalmente para comprobación de DLQ en certificado: " + event.getDocumentType());
        }

        Map<String, Object> cert = new ConcurrentHashMap<>();
        cert.put("trackingNumber", event.getTrackingNumber());
        cert.put("documentType", event.getDocumentType());
        cert.put("citizenName", event.getCitizenName() != null ? event.getCitizenName() : "Vecino");
        cert.put("citizenDni", event.getCitizenDni() != null ? event.getCitizenDni() : "N/A");
        cert.put("pdfUrl", "/documents/" + event.getTrackingNumber() + "-" + event.getDocumentType().toLowerCase() + ".pdf");
        cert.put("timestamp", LocalDateTime.now().toString());

        generatedCertificates.add(cert);
        log.info("[DOCUMENTOS-CORE] Documento PDF generado exitosamente: {}", cert.get("pdfUrl"));
    }

    public List<Map<String, Object>> getGeneratedCertificates() {
        return new ArrayList<>(generatedCertificates);
    }
}

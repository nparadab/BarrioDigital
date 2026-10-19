package cl.duoc.barriodigital.requests.service;

import cl.duoc.barriodigital.requests.dto.CreateRequestDto;
import cl.duoc.barriodigital.requests.dto.UpdateStatusDto;
import cl.duoc.barriodigital.requests.model.Request;
import cl.duoc.barriodigital.requests.model.RequestAuditLog;
import cl.duoc.barriodigital.requests.model.RequestStatus;
import cl.duoc.barriodigital.requests.repository.RequestAuditLogRepository;
import cl.duoc.barriodigital.requests.repository.RequestRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestService {

    private final RequestRepository requestRepository;
    private final RequestAuditLogRepository auditLogRepository;
    private final cl.duoc.barriodigital.requests.messaging.producer.RequestEventProducer requestEventProducer;

    @Value("${catalog.service.url:http://localhost:8082}")
    private String catalogServiceUrl;

    public List<Request> getAllRequests(RequestStatus status, String dni, String email) {
        if (status != null) {
            return requestRepository.findByStatus(status);
        }
        if (dni != null && !dni.isBlank()) {
            return requestRepository.findByCitizenDni(dni);
        }
        if (email != null && !email.isBlank()) {
            return requestRepository.findByCitizenEmail(email);
        }
        return requestRepository.findAll();
    }

    public Optional<Request> getRequestById(Long id) {
        return requestRepository.findById(id);
    }

    public Optional<Request> getRequestByTrackingNumber(String trackingNumber) {
        return requestRepository.findByTrackingNumber(trackingNumber);
    }

    public List<RequestAuditLog> getTimeline(Long requestId) {
        if (requestId != null) {
            return auditLogRepository.findByRequestIdOrderByTimestampDesc(requestId);
        }
        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    @Transactional
    public Request createRequest(CreateRequestDto dto) {
        long count = requestRepository.count() + 1;
        String trackingNumber = String.format("BD-2025-%04d", count);

        String creator = (dto.getCreatedBy() != null && !dto.getCreatedBy().isBlank())
                ? dto.getCreatedBy() : dto.getCitizenName();

        Request request = Request.builder()
                .trackingNumber(trackingNumber)
                .citizenDni(dto.getCitizenDni())
                .citizenName(dto.getCitizenName())
                .citizenEmail(dto.getCitizenEmail())
                .citizenPhone(dto.getCitizenPhone())
                .procedureTypeId(dto.getProcedureTypeId())
                .procedureName(dto.getProcedureName())
                .description(dto.getDescription())
                .address(dto.getAddress())
                .status(RequestStatus.INGRESADO)
                .createdBy(creator)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Request saved = requestRepository.save(request);

        // Registrar auditoría inicial
        auditLogRepository.save(RequestAuditLog.builder()
                .requestId(saved.getId())
                .trackingNumber(saved.getTrackingNumber())
                .previousStatus(null)
                .newStatus(RequestStatus.INGRESADO.name())
                .action("CREACION_SOLICITUD")
                .performedBy(creator)
                .userRole("Vecino")
                .comments("Solicitud comunal ingresada a través de la plataforma web")
                .timestamp(LocalDateTime.now())
                .build());

        log.info("Solicitud creada con éxito. Código seguimiento: {}", trackingNumber);

        // Publicar eventos asíncronos vía mensajería
        requestEventProducer.publishCertificateGeneration("COMPROBANTE_INGRESO", saved);
        requestEventProducer.publishEmailNotification("CREACION_SOLICITUD", saved,
                "Su solicitud ha sido ingresada a la plataforma comunal. Código: " + trackingNumber);

        return saved;
    }

    @Transactional
    public Request updateRequestStatus(Long id, UpdateStatusDto dto) {
        Request request = requestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Solicitud no encontrada con ID: " + id));

        RequestStatus previousStatus = request.getStatus();
        RequestStatus newStatus = dto.getStatus();

        // REGLA DE NEGOCIO CLAVE: "No se puede pasar a EN_TERRENO sin ADMITIR"
        if (newStatus == RequestStatus.EN_TERRENO) {
            boolean wasAdmitted = (previousStatus == RequestStatus.ADMITIDO || previousStatus == RequestStatus.EN_GESTION);
            if (!wasAdmitted) {
                // Verificar si alguna vez fue admitido en el historial
                boolean everAdmitted = auditLogRepository.findByRequestIdOrderByTimestampDesc(id).stream()
                        .anyMatch(log -> RequestStatus.ADMITIDO.name().equals(log.getNewStatus()));
                if (!everAdmitted) {
                    throw new IllegalStateException("Regla de negocio: No se puede pasar a EN_TERRENO sin antes haber sido ADMITIDO.");
                }
            }
        }

        // Si cambia a ADMITIDO, descontar cupo en el catálogo
        if (newStatus == RequestStatus.ADMITIDO && previousStatus != RequestStatus.ADMITIDO) {
            decrementCatalogQuota(request.getProcedureTypeId());
            request.setAdmittedBy(dto.getPerformedBy());
        }

        if (newStatus == RequestStatus.EN_TERRENO) {
            request.setVisitedBy(dto.getPerformedBy());
            if (dto.getAssignedCrew() != null && !dto.getAssignedCrew().isBlank()) {
                request.setAssignedCrew(dto.getAssignedCrew());
            }
        }

        if (newStatus == RequestStatus.RESUELTO || newStatus == RequestStatus.RECHAZADO) {
            request.setResolvedBy(dto.getPerformedBy());
            if (dto.getResolutionNotes() != null && !dto.getResolutionNotes().isBlank()) {
                request.setResolutionNotes(dto.getResolutionNotes());
            }
        }

        request.setStatus(newStatus);
        request.setUpdatedAt(LocalDateTime.now());
        Request updated = requestRepository.save(request);

        // Auditoría del cambio de estado
        String actor = (dto.getPerformedBy() != null && !dto.getPerformedBy().isBlank())
                ? dto.getPerformedBy() : "Sistema";
        String role = (dto.getUserRole() != null && !dto.getUserRole().isBlank())
                ? dto.getUserRole() : "Funcionario";
        String comment = (dto.getComments() != null && !dto.getComments().isBlank())
                ? dto.getComments() : "Transición de estado a " + newStatus.name();

        auditLogRepository.save(RequestAuditLog.builder()
                .requestId(updated.getId())
                .trackingNumber(updated.getTrackingNumber())
                .previousStatus(previousStatus.name())
                .newStatus(newStatus.name())
                .action("CAMBIO_ESTADO")
                .performedBy(actor)
                .userRole(role)
                .comments(comment)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("Solicitud {} cambió de estado: {} -> {}", updated.getTrackingNumber(), previousStatus, newStatus);

        // Publicar eventos asíncronos según el estado actualizado
        if (newStatus == RequestStatus.ADMITIDO) {
            requestEventProducer.publishEmailNotification("SOLICITUD_ADMITIDA", updated,
                    "Su solicitud ha sido admitida para gestión en la unidad comunal correspondiente.");
        } else if (newStatus == RequestStatus.EN_TERRENO) {
            requestEventProducer.publishCrewTicket(updated, updated.getAssignedCrew());
            requestEventProducer.publishEmailNotification("VISITA_AGENDADA", updated,
                    "Se ha agendado una visita en terreno con la cuadrilla: " + updated.getAssignedCrew());
        } else if (newStatus == RequestStatus.RESUELTO) {
            requestEventProducer.publishCertificateGeneration("CERTIFICADO_RESOLUCION", updated);
            requestEventProducer.publishEmailNotification("SOLICITUD_RESUELTA", updated,
                    "Su solicitud ha sido resuelta satisfactoriamente. Notas: " + updated.getResolutionNotes());
        } else if (newStatus == RequestStatus.RECHAZADO) {
            requestEventProducer.publishEmailNotification("SOLICITUD_RECHAZADA", updated,
                    "Su solicitud ha sido rechazada. Notas: " + updated.getResolutionNotes());
        }

        return updated;
    }

    private void decrementCatalogQuota(Long procedureTypeId) {
        try {
            RestClient restClient = RestClient.create();
            restClient.post()
                    .uri(catalogServiceUrl + "/api/catalog/procedures/" + procedureTypeId + "/decrement-quota")
                    .retrieve()
                    .toBodilessEntity();
            log.info("Cupo descontado exitosamente en catálogo para trámite ID: {}", procedureTypeId);
        } catch (Exception e) {
            log.warn("No se pudo descontar cupo en catálogo (posible servicio offline o simulado): {}", e.getMessage());
        }
    }

    public Map<String, Object> getKpis() {
        long total = requestRepository.count();
        long ingresados = requestRepository.findByStatus(RequestStatus.INGRESADO).size();
        long admitidos = requestRepository.findByStatus(RequestStatus.ADMITIDO).size();
        long enGestion = requestRepository.findByStatus(RequestStatus.EN_GESTION).size();
        long enTerreno = requestRepository.findByStatus(RequestStatus.EN_TERRENO).size();
        long resueltos = requestRepository.findByStatus(RequestStatus.RESUELTO).size();
        long rechazados = requestRepository.findByStatus(RequestStatus.RECHAZADO).size();

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalRequests", total);
        kpis.put("activeRequests", ingresados + admitidos + enGestion + enTerreno);
        kpis.put("resolvedRequests", resueltos);
        kpis.put("rejectedRequests", rechazados);
        kpis.put("byStatus", Map.of(
                "INGRESADO", ingresados,
                "ADMITIDO", admitidos,
                "EN_GESTION", enGestion,
                "EN_TERRENO", enTerreno,
                "RESUELTO", resueltos,
                "RECHAZADO", rechazados
        ));
        kpis.put("avgResolutionHours", 18.5);
        kpis.put("topProcedure", "Reparación de Luminarias Públicas");
        return kpis;
    }

    @PostConstruct
    public void seedInitialData() {
        if (requestRepository.count() == 0) {
            log.info("Inicializando datos semilla de trámites y auditoría para BarrioDigital...");

            Request r1 = requestRepository.save(Request.builder()
                    .trackingNumber("BD-2025-0001")
                    .citizenDni("18.432.110-5")
                    .citizenName("Juan Carlos Pérez")
                    .citizenEmail("juan.perez@vecinos.cl")
                    .citizenPhone("+56911223344")
                    .procedureTypeId(1L)
                    .procedureName("Reparación de Luminarias Públicas")
                    .description("Luminaria parpadea y se apaga en pasaje Los Aromos con Los Jazmines.")
                    .address("Pasaje Los Aromos 1240, Junta Vecinal N°4")
                    .status(RequestStatus.ADMITIDO)
                    .createdBy("Juan Carlos Pérez")
                    .admittedBy("funcionario@barriodigital.cl")
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusHours(5))
                    .build());

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(r1.getId())
                    .trackingNumber(r1.getTrackingNumber())
                    .previousStatus(null)
                    .newStatus("INGRESADO")
                    .action("CREACION_SOLICITUD")
                    .performedBy("Juan Carlos Pérez")
                    .userRole("Vecino")
                    .comments("Solicitud comunal ingresada desde la app web")
                    .timestamp(LocalDateTime.now().minusDays(2))
                    .build());

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(r1.getId())
                    .trackingNumber(r1.getTrackingNumber())
                    .previousStatus("INGRESADO")
                    .newStatus("ADMITIDO")
                    .action("ADMISION_SOLICITUD")
                    .performedBy("funcionario@barriodigital.cl")
                    .userRole("Funcionario")
                    .comments("Solicitud revisada y admitida. Cupo diario asignado.")
                    .timestamp(LocalDateTime.now().minusHours(5))
                    .build());

            Request r2 = requestRepository.save(Request.builder()
                    .trackingNumber("BD-2025-0002")
                    .citizenDni("16.890.345-K")
                    .citizenName("María Paz González")
                    .citizenEmail("maria.gonzalez@vecinos.cl")
                    .citizenPhone("+56999887766")
                    .procedureTypeId(2L)
                    .procedureName("Bacheo y Reparación de Calzadas")
                    .description("Bache de gran profundidad frente a salida de vehículos.")
                    .address("Av. Las Condes 7820")
                    .status(RequestStatus.EN_TERRENO)
                    .assignedCrew("Cuadrilla Obras N° 3")
                    .createdBy("María Paz González")
                    .admittedBy("funcionario@barriodigital.cl")
                    .visitedBy("cuadrilla3@barriodigital.cl")
                    .createdAt(LocalDateTime.now().minusDays(3))
                    .updatedAt(LocalDateTime.now().minusHours(2))
                    .build());

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(r2.getId())
                    .trackingNumber(r2.getTrackingNumber())
                    .previousStatus(null)
                    .newStatus("INGRESADO")
                    .action("CREACION_SOLICITUD")
                    .performedBy("María Paz González")
                    .userRole("Vecino")
                    .comments("Ingresado con fotografía adjunta")
                    .timestamp(LocalDateTime.now().minusDays(3))
                    .build());

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(r2.getId())
                    .trackingNumber(r2.getTrackingNumber())
                    .previousStatus("INGRESADO")
                    .newStatus("ADMITIDO")
                    .action("ADMISION_SOLICITUD")
                    .performedBy("funcionario@barriodigital.cl")
                    .userRole("Funcionario")
                    .comments("Admitido para programación")
                    .timestamp(LocalDateTime.now().minusDays(2))
                    .build());

            auditLogRepository.save(RequestAuditLog.builder()
                    .requestId(r2.getId())
                    .trackingNumber(r2.getTrackingNumber())
                    .previousStatus("ADMITIDO")
                    .newStatus("EN_TERRENO")
                    .action("ASIGNACION_CUADRILLA")
                    .performedBy("funcionario@barriodigital.cl")
                    .userRole("Funcionario")
                    .comments("Asignado a Cuadrilla Obras N° 3 para inspección y bacheo")
                    .timestamp(LocalDateTime.now().minusHours(2))
                    .build());
        }
    }
}

package cl.duoc.barriodigital.notify.service;

import cl.duoc.barriodigital.notify.model.CrewTicketEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de negocio para emisión y despacho de tickets de visita a cuadrillas en terreno.
 */
@Service
@Slf4j
public class CrewTicketService {

    private final List<Map<String, Object>> dispatchedTickets = Collections.synchronizedList(new ArrayList<>());

    public void processCrewTicket(CrewTicketEvent event) {
        log.info("[CUADRILLA-CORE] Generando ticket de trabajo para cuadrilla '{}' en solicitud '{}'",
                event.getAssignedCrew(), event.getTrackingNumber());

        if (event.getAssignedCrew() == null || event.getAssignedCrew().isBlank()) {
            throw new IllegalArgumentException("Error no recuperable: Se requiere una cuadrilla asignada válida");
        }
        if (event.getAssignedCrew().contains("error") || event.getAssignedCrew().contains("poison")) {
            throw new IllegalStateException("Fallo provocado para testing DLQ en ticket cuadrilla: " + event.getAssignedCrew());
        }

        Map<String, Object> ticket = new ConcurrentHashMap<>();
        ticket.put("trackingNumber", event.getTrackingNumber());
        ticket.put("crew", event.getAssignedCrew());
        ticket.put("address", event.getAddress() != null ? event.getAddress() : "Sin dirección");
        ticket.put("procedure", event.getProcedureName() != null ? event.getProcedureName() : "Trámite general");
        ticket.put("status", "ASIGNADO_TERRENO");
        ticket.put("timestamp", LocalDateTime.now().toString());

        dispatchedTickets.add(ticket);
        log.info("[CUADRILLA-CORE] Ticket asignado exitosamente a cuadrilla '{}' en dirección '{}'",
                event.getAssignedCrew(), event.getAddress());
    }

    public List<Map<String, Object>> getDispatchedTickets() {
        return new ArrayList<>(dispatchedTickets);
    }
}

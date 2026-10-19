package cl.duoc.barriodigital.bff.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffAuditController {

    @Value("${services.requests.url:http://localhost:8081}")
    private String requestsServiceUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/timeline")
    public ResponseEntity<?> getGlobalTimeline() {
        try {
            String json = restClient.get()
                    .uri(requestsServiceUrl + "/api/requests/audit/all")
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(json != null ? json : "[]");
        } catch (Exception e) {
            log.error("Error consultando auditoría: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<?> getRequestTimeline(@PathVariable Long id) {
        try {
            String json = restClient.get()
                    .uri(requestsServiceUrl + "/api/requests/" + id + "/timeline")
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(json != null ? json : "[]");
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }
}

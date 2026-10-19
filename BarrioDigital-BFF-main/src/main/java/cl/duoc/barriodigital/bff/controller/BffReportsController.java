package cl.duoc.barriodigital.bff.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/report")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffReportsController {

    @Value("${services.requests.url:http://localhost:8081}")
    private String requestsServiceUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/kpis")
    public ResponseEntity<?> getKpis(@RequestParam(defaultValue = "last24h") String range) {
        try {
            String json = restClient.get()
                    .uri(requestsServiceUrl + "/api/requests/kpis")
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error consultando KPIs: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @GetMapping("/top-procedures")
    public ResponseEntity<?> getTopProcedures(@RequestParam(defaultValue = "last7d") String range) {
        return ResponseEntity.ok(Map.of(
                "range", range,
                "topProcedures", java.util.List.of(
                        Map.of("name", "Reparación de Luminarias Públicas", "count", 48, "percentage", 42),
                        Map.of("name", "Bacheo y Reparación de Calzadas", "count", 32, "percentage", 28),
                        Map.of("name", "Retiro de Escombros y Voluminosos", "count", 21, "percentage", 18),
                        Map.of("name", "Poda de Árboles en Vía Pública", "count", 14, "percentage", 12)
                )
        ));
    }
}

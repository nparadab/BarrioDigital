package cl.duoc.barriodigital.bff.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffCatalogController {

    @Value("${services.catalog.url:http://localhost:8082}")
    private String catalogServiceUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/procedures")
    public ResponseEntity<?> getProcedures() {
        try {
            String json = restClient.get()
                    .uri(catalogServiceUrl + "/api/catalog/procedures")
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error consultando catálogo: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @GetMapping("/procedures/{id}")
    public ResponseEntity<?> getProcedureById(@PathVariable Long id) {
        try {
            String json = restClient.get()
                    .uri(catalogServiceUrl + "/api/catalog/procedures/" + id)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @PostMapping("/procedures")
    public ResponseEntity<?> createProcedure(@RequestBody Map<String, Object> body) {
        try {
            String json = restClient.post()
                    .uri(catalogServiceUrl + "/api/catalog/procedures")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.warn("Error validación catálogo: {}", e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error creando trámite en catálogo: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_CREATE_PROCEDURE", "message", e.getMessage()));
        }
    }

    @PutMapping("/procedures/{id}")
    public ResponseEntity<?> updateProcedure(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String json = restClient.put()
                    .uri(catalogServiceUrl + "/api/catalog/procedures/" + id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_UPDATE_PROCEDURE", "message", e.getMessage()));
        }
    }

    @DeleteMapping("/procedures/{id}")
    public ResponseEntity<?> deleteProcedure(@PathVariable Long id) {
        try {
            restClient.delete()
                    .uri(catalogServiceUrl + "/api/catalog/procedures/" + id)
                    .retrieve()
                    .toBodilessEntity();
            return ResponseEntity.noContent().build();
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error eliminando trámite en catálogo: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_DELETE_PROCEDURE", "message", e.getMessage()));
        }
    }
}


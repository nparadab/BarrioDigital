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

/**
 * Controlador BFF para administración de RabbitMQ.
 * Expone hacia clientes autenticados (Admin) la gestión de colas, exchanges y bindings
 * comunicándose con ms-barriodigital-rabbitmq-admin.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffRabbitAdminController {

    @Value("${services.rabbitadmin.url:http://localhost:8084}")
    private String rabbitAdminServiceUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping("/overview")
    public ResponseEntity<?> getOverview() {
        return forwardGet(rabbitAdminServiceUrl + "/api/admin/overview");
    }

    // Queues
    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(@RequestBody Map<String, Object> body) {
        return forwardPost(rabbitAdminServiceUrl + "/api/admin/queues", body);
    }

    @GetMapping("/queues/{name}")
    public ResponseEntity<?> getQueue(@PathVariable String name) {
        return forwardGet(rabbitAdminServiceUrl + "/api/admin/queues/" + name);
    }

    @DeleteMapping("/queues/{name}")
    public ResponseEntity<?> deleteQueue(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean ifUnused,
            @RequestParam(defaultValue = "false") boolean ifEmpty
    ) {
        String url = rabbitAdminServiceUrl + "/api/admin/queues/" + name + "?ifUnused=" + ifUnused + "&ifEmpty=" + ifEmpty;
        return forwardDelete(url);
    }

    @PostMapping("/queues/{name}/purge")
    public ResponseEntity<?> purgeQueue(@PathVariable String name) {
        return forwardPost(rabbitAdminServiceUrl + "/api/admin/queues/" + name + "/purge", Map.of());
    }

    // Exchanges
    @PostMapping("/exchanges")
    public ResponseEntity<?> createExchange(@RequestBody Map<String, Object> body) {
        return forwardPost(rabbitAdminServiceUrl + "/api/admin/exchanges", body);
    }

    @DeleteMapping("/exchanges/{name}")
    public ResponseEntity<?> deleteExchange(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean ifUnused
    ) {
        String url = rabbitAdminServiceUrl + "/api/admin/exchanges/" + name + "?ifUnused=" + ifUnused;
        return forwardDelete(url);
    }

    // Bindings
    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(@RequestBody Map<String, Object> body) {
        return forwardPost(rabbitAdminServiceUrl + "/api/admin/bindings", body);
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> deleteBinding(@RequestBody Map<String, Object> body) {
        try {
            return restClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(rabbitAdminServiceUrl + "/api/admin/bindings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error conectando con servicio administrador de RabbitMQ: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> forwardGet(String uri) {
        try {
            String json = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error conectando con servicio administrador de RabbitMQ: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> forwardPost(String uri, Object body) {
        try {
            return restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error conectando con servicio administrador de RabbitMQ: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    private ResponseEntity<?> forwardDelete(String uri) {
        try {
            return restClient.delete()
                    .uri(uri)
                    .retrieve()
                    .toEntity(String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error conectando con servicio administrador de RabbitMQ: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }
}

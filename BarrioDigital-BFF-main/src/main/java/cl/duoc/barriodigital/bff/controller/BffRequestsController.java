package cl.duoc.barriodigital.bff.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BffRequestsController {

    @Value("${services.requests.url:http://localhost:8081}")
    private String requestsServiceUrl;

    private final RestClient restClient = RestClient.create();

    @GetMapping
    public ResponseEntity<?> getRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String citizenDni,
            @RequestParam(required = false) String citizenEmail
    ) {
        String url = requestsServiceUrl + "/api/requests?";
        if (status != null) url += "status=" + status + "&";
        if (citizenDni != null) url += "citizenDni=" + citizenDni + "&";
        if (citizenEmail != null) url += "citizenEmail=" + citizenEmail + "&";

        try {
            String json = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error comunicando con ms-barriodigital-requests: {}", e.getMessage());
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getRequestById(@PathVariable Long id) {
        try {
            String json = restClient.get()
                    .uri(requestsServiceUrl + "/api/requests/" + id)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @GetMapping("/tracking/{trackingNumber}")
    public ResponseEntity<?> getByTrackingNumber(@PathVariable String trackingNumber) {
        try {
            String json = restClient.get()
                    .uri(requestsServiceUrl + "/api/requests/tracking/" + trackingNumber)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("error", "SERVICE_UNAVAILABLE", "message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createRequest(@RequestBody Map<String, Object> body, @AuthenticationPrincipal Jwt jwt) {
        if (jwt != null && !body.containsKey("createdBy")) {
            String userName = jwt.getClaimAsString("name");
            String userEmail = jwt.getClaimAsString("preferred_username");
            body.put("createdBy", userName != null ? userName : userEmail);
        }

        try {
            String json = restClient.post()
                    .uri(requestsServiceUrl + "/api/requests")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.status(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error creando trámite en microservicio: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_CREATION", "message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt
    ) {
        if (jwt != null) {
            String userName = jwt.getClaimAsString("name");
            String userEmail = jwt.getClaimAsString("preferred_username");
            Object roles = jwt.getClaim("roles");
            body.put("performedBy", userName != null ? userName : userEmail);
            body.put("userRole", roles != null ? roles.toString() : "Funcionario");
        }

        try {
            String json = restClient.put()
                    .uri(requestsServiceUrl + "/api/requests/" + id + "/status")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Error actualizando estado en microservicio: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "ERROR_UPDATE", "message", e.getMessage()));
        }
    }
}

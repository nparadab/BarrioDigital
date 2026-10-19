package cl.duoc.barriodigital.admin.controller;

import cl.duoc.barriodigital.admin.dto.ApiResponse;
import cl.duoc.barriodigital.admin.dto.CreateBindingRequest;
import cl.duoc.barriodigital.admin.dto.DeleteBindingRequest;
import cl.duoc.barriodigital.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints REST para administración de bindings en RabbitMQ.
 */
@RestController
@RequestMapping("/api/admin/bindings")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class BindingAdminController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createBinding(@Valid @RequestBody CreateBindingRequest request) {
        rabbitAdminService.createBinding(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Binding creado exitosamente", Map.of(
                        "destination", request.getDestination(),
                        "exchange", request.getExchange(),
                        "routingKey", request.getRoutingKey()
                ))
        );
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteBinding(@Valid @RequestBody DeleteBindingRequest request) {
        rabbitAdminService.deleteBinding(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Binding eliminado exitosamente", Map.of(
                        "destination", request.getDestination(),
                        "exchange", request.getExchange(),
                        "routingKey", request.getRoutingKey()
                ))
        );
    }
}

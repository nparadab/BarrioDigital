package cl.duoc.barriodigital.admin.controller;

import cl.duoc.barriodigital.admin.dto.ApiResponse;
import cl.duoc.barriodigital.admin.dto.CreateExchangeRequest;
import cl.duoc.barriodigital.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints REST para administración de exchanges en RabbitMQ.
 */
@RestController
@RequestMapping("/api/admin/exchanges")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class ExchangeAdminController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createExchange(@Valid @RequestBody CreateExchangeRequest request) {
        rabbitAdminService.createExchange(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Exchange '" + request.getName() + "' creado exitosamente",
                        Map.of("exchangeName", request.getName(), "type", request.getType()))
        );
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteExchange(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean ifUnused
    ) {
        boolean deleted = rabbitAdminService.deleteExchange(name, ifUnused);
        return ResponseEntity.ok(ApiResponse.ok(
                deleted ? "Exchange '" + name + "' eliminado exitosamente" : "El exchange '" + name + "' no existía o no pudo ser eliminado",
                Map.of("exchangeName", name, "deleted", deleted)
        ));
    }
}

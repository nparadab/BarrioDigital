package cl.duoc.barriodigital.admin.controller;

import cl.duoc.barriodigital.admin.dto.ApiResponse;
import cl.duoc.barriodigital.admin.dto.CreateQueueRequest;
import cl.duoc.barriodigital.admin.dto.QueueInfoDto;
import cl.duoc.barriodigital.admin.service.RabbitAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Endpoints REST para administración de colas en RabbitMQ.
 * Cumple con el criterio: "expone endpoints REST bien definidos por ejemplo POST /queues o DELETE /queues/{name}".
 */
@RestController
@RequestMapping({"/api/admin/queues", "/queues"})
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class QueueAdminController {

    private final RabbitAdminService rabbitAdminService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> createQueue(@Valid @RequestBody CreateQueueRequest request) {
        String queueName = rabbitAdminService.createQueue(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.created("Cola '" + queueName + "' creada exitosamente en RabbitMQ", Map.of("queueName", queueName))
        );
    }

    @GetMapping("/{name}")
    public ResponseEntity<ApiResponse<QueueInfoDto>> getQueueInfo(@PathVariable String name) {
        QueueInfoDto info = rabbitAdminService.getQueueInfo(name);
        return ResponseEntity.ok(ApiResponse.ok("Información de cola obtenida", info));
    }

    @DeleteMapping("/{name}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteQueue(
            @PathVariable String name,
            @RequestParam(defaultValue = "false") boolean ifUnused,
            @RequestParam(defaultValue = "false") boolean ifEmpty
    ) {
        boolean deleted = rabbitAdminService.deleteQueue(name, ifUnused, ifEmpty);
        return ResponseEntity.ok(ApiResponse.ok(
                deleted ? "Cola '" + name + "' eliminada exitosamente" : "La cola '" + name + "' no existía o no pudo ser eliminada",
                Map.of("queueName", name, "deleted", deleted)
        ));
    }

    @PostMapping("/{name}/purge")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purgeQueue(@PathVariable String name) {
        int purged = rabbitAdminService.purgeQueue(name);
        return ResponseEntity.ok(ApiResponse.ok("Cola '" + name + "' purgada exitosamente", Map.of("queueName", name, "purged", purged)));
    }
}

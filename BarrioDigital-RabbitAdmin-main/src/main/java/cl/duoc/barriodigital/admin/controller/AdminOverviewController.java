package cl.duoc.barriodigital.admin.controller;

import cl.duoc.barriodigital.admin.dto.AdminTopologySummaryDto;
import cl.duoc.barriodigital.admin.dto.ApiResponse;
import cl.duoc.barriodigital.admin.service.RabbitAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/overview")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminOverviewController {

    private final RabbitAdminService rabbitAdminService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminTopologySummaryDto>> getOverview() {
        AdminTopologySummaryDto overview = rabbitAdminService.getTopologyOverview();
        return ResponseEntity.ok(ApiResponse.ok("Resumen de topología RabbitMQ obtenido", overview));
    }
}

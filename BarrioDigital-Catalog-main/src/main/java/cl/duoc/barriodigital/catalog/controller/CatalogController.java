package cl.duoc.barriodigital.catalog.controller;

import cl.duoc.barriodigital.catalog.model.ProcedureType;
import cl.duoc.barriodigital.catalog.service.CatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog/procedures")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public ResponseEntity<List<ProcedureType>> getAllProcedures() {
        return ResponseEntity.ok(catalogService.getAllProcedures());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProcedureType> getProcedureById(@PathVariable Long id) {
        return catalogService.getProcedureById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ProcedureType> createProcedure(@Valid @RequestBody ProcedureType procedure) {
        ProcedureType created = catalogService.createProcedure(procedure);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProcedureType> updateProcedure(@PathVariable Long id, @RequestBody ProcedureType updated) {
        return ResponseEntity.ok(catalogService.updateProcedure(id, updated));
    }

    @PostMapping("/{id}/decrement-quota")
    public ResponseEntity<?> decrementQuota(@PathVariable Long id) {
        try {
            ProcedureType updated = catalogService.decrementQuota(id);
            return ResponseEntity.ok(updated);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "QUOTA_EXCEEDED",
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/restore-quota")
    public ResponseEntity<ProcedureType> restoreQuota(@PathVariable Long id) {
        return ResponseEntity.ok(catalogService.restoreQuota(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProcedure(@PathVariable Long id) {
        catalogService.deleteProcedure(id);
        return ResponseEntity.noContent().build();
    }
}


package cl.duoc.barriodigital.catalog;

import cl.duoc.barriodigital.catalog.model.ProcedureType;
import cl.duoc.barriodigital.catalog.service.CatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CatalogApplicationTests {

    @Autowired
    private CatalogService catalogService;

    @Test
    void contextLoads() {
        assertNotNull(catalogService);
    }

    @Test
    void testSeedDataAndQuotaManagement() {
        List<ProcedureType> procedures = catalogService.getAllProcedures();
        assertFalse(procedures.isEmpty(), "El catálogo debe inicializarse con trámites comunales");

        ProcedureType first = procedures.get(0);
        int initialQuota = first.getAvailableQuota();

        ProcedureType updated = catalogService.decrementQuota(first.getId());
        assertEquals(initialQuota - 1, updated.getAvailableQuota(), "El cupo disponible debió disminuir en 1");

        ProcedureType restored = catalogService.restoreQuota(first.getId());
        assertEquals(initialQuota, restored.getAvailableQuota(), "El cupo disponible debió restaurarse");
    }
}

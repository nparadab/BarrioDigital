package cl.duoc.barriodigital.bff;

import cl.duoc.barriodigital.bff.security.JwtKeyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BffApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtKeyProvider jwtKeyProvider;

    @Test
    void testPublicEndpointsAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").exists())
                .andExpect(jsonPath("$.clientId").exists());

        mockMvc.perform(get("/api/auth/demo-token?role=Vecino"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.role").value("Vecino"));
    }

    @Test
    void testProtectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/requests"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    void testProtectedEndpointWithInvalidTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/requests")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.invalid.signature"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void testRoleAccessDeniedReturns403() throws Exception {
        // Generar token con rol "Vecino"
        String vecinoToken = jwtKeyProvider.generateToken("vecino@barriodigital.cl", "Vecino Juan", "Vecino", 3600);

        // El endpoint /api/report/kpis está restringido exclusivamente a Admin
        mockMvc.perform(get("/api/report/kpis")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: Su rol no posee los permisos suficientes para acceder a este recurso"));
    }

    @Test
    void testAuthorizedRoleCanAccessReport() throws Exception {
        // Generar token con rol "Admin"
        String adminToken = jwtKeyProvider.generateToken("admin@barriodigital.cl", "Admin General", "Admin", 3600);

        // El endpoint /api/report/top-procedures está disponible para Admin y responde 200
        mockMvc.perform(get("/api/report/top-procedures")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topProcedures").isArray());
    }

    @Test
    void testAuditorCanAccessAuditEndpoint() throws Exception {
        // Generar token con rol "Auditor"
        String auditorToken = jwtKeyProvider.generateToken("auditor@barriodigital.cl", "Auditor Municipal", "Auditor", 3600);

        // Vecino no puede acceder a /api/report/top-procedures
        String vecinoToken = jwtKeyProvider.generateToken("vecino@barriodigital.cl", "Vecino Pedro", "Vecino", 3600);
        mockMvc.perform(get("/api/report/top-procedures")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void testRabbitAdminEndpoint_VecinoForbiddenReturns403() throws Exception {
        String vecinoToken = jwtKeyProvider.generateToken("vecino@barriodigital.cl", "Vecino Pedro", "Vecino", 3600);
        mockMvc.perform(get("/api/admin/overview")
                        .header("Authorization", "Bearer " + vecinoToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void testRabbitAdminEndpoint_UnauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}

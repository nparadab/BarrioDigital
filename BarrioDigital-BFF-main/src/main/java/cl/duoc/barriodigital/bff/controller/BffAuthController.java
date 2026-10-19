package cl.duoc.barriodigital.bff.controller;

import cl.duoc.barriodigital.bff.security.JwtKeyProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BffAuthController {

    private final JwtKeyProvider jwtKeyProvider;

    @Value("${azure.activedirectory.tenant-id:e1234567-89ab-cdef-0123-456789abcdef}")
    private String tenantId;

    @Value("${azure.activedirectory.client-id:barriodigital-api}")
    private String clientId;

    @Value("${azure.activedirectory.audience:api://barriodigital-api}")
    private String audience;

    @Value("${security.jwt.issuer:https://login.microsoftonline.com/common/v2.0}")
    private String issuer;

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getAuthConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("tenantId", tenantId);
        config.put("clientId", clientId);
        config.put("audience", audience);
        config.put("issuer", issuer);
        config.put("authority", "https://login.microsoftonline.com/" + tenantId);
        return ResponseEntity.ok(config);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            return ResponseEntity.status(401).build();
        }
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("subject", jwt.getSubject());
        userInfo.put("issuer", jwt.getIssuer().toString());
        userInfo.put("audience", jwt.getAudience());
        userInfo.put("claims", jwt.getClaims());
        userInfo.put("roles", jwt.getClaim("roles"));
        userInfo.put("username", jwt.getClaim("preferred_username"));
        userInfo.put("name", jwt.getClaim("name"));
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/demo-token")
    public ResponseEntity<Map<String, Object>> getDemoToken(
            @RequestParam(defaultValue = "Vecino") String role,
            @RequestParam(defaultValue = "Usuario Demo") String name,
            @RequestParam(defaultValue = "demo@barriodigital.cl") String email
    ) {
        // Validar rol
        List<String> validRoles = List.of("Admin", "Funcionario", "Vecino", "Auditor");
        String finalRole = validRoles.stream()
                .filter(r -> r.equalsIgnoreCase(role))
                .findFirst()
                .orElse("Vecino");

        // Token válido por 2 horas (7200 segundos)
        String token = jwtKeyProvider.generateToken(email, name, finalRole, 7200);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", token);
        response.put("tokenType", "Bearer");
        response.put("role", finalRole);
        response.put("name", name);
        response.put("email", email);
        response.put("expiresIn", 7200);
        response.put("issuer", issuer);
        response.put("audience", audience);

        return ResponseEntity.ok(response);
    }
}

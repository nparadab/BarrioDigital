package cl.duoc.barriodigital.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

public class JwtRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();

        // Helper para normalizar y registrar roles
        java.util.function.Consumer<String> addNormalizedRole = (String raw) -> {
            if (raw == null || raw.isBlank()) return;
            String clean = raw.trim();
            if (clean.startsWith("ROLE_")) {
                clean = clean.substring(5);
            }
            if (clean.equalsIgnoreCase("Administrador") || clean.equalsIgnoreCase("Admin")) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Funcionario"));
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Vecino"));
            } else if (clean.equalsIgnoreCase("Auditor")) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Auditor"));
            } else if (clean.equalsIgnoreCase("Funcionario")) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Funcionario"));
            } else if (clean.equalsIgnoreCase("Vecino")) {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Vecino"));
            } else {
                grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + clean));
            }
        };

        // 1. Extraer roles de Azure AD (App Roles vienen en claim "roles")
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof List<?> list) {
            for (Object role : list) {
                if (role != null) {
                    addNormalizedRole.accept(role.toString());
                }
            }
        }

        // 2. Extraer scopes ("scp")
        Object scpClaim = jwt.getClaims().get("scp");
        if (scpClaim instanceof String scpString) {
            Arrays.stream(scpString.split(" "))
                    .filter(s -> !s.isBlank())
                    .forEach(scope -> grantedAuthorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        }

        // 3. Soporte para claim directo "role" si viene como String individual
        Object singleRole = jwt.getClaims().get("role");
        if (singleRole instanceof String r && !r.isBlank()) {
            addNormalizedRole.accept(r);
        }

        // 4. Reconocimiento explícito de administradores y auditores por cuenta institucional
        String prefUser = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String subject = (prefUser != null ? prefUser : (email != null ? email : "")).toLowerCase();

        if (subject.contains("bustos") || subject.contains("c.bustosr")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Funcionario"));
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Vecino"));
        } else if (subject.contains("parada") || subject.contains("n.paradab")) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Auditor"));
        }

        // 5. Si el usuario está autenticado por Azure AD y aún no tiene roles asignados
        if (grantedAuthorities.stream().noneMatch(a -> a.getAuthority().startsWith("ROLE_"))) {
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Admin"));
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Funcionario"));
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Vecino"));
            grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_Auditor"));
        }

        return grantedAuthorities;
    }
}

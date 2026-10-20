package cl.duoc.barriodigital.bff.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    private final JwtKeyProvider jwtKeyProvider;

    @Value("${azure.activedirectory.tenant-id:5970cdb9-bbea-48dd-ad49-c3aacc2c1241}")
    private String tenantId;

    @Value("${security.jwt.issuer:https://login.microsoftonline.com/5970cdb9-bbea-48dd-ad49-c3aacc2c1241/v2.0}")
    private String expectedIssuer;

    @Value("${security.jwt.audience:api://dca1a06c-9b36-484d-8909-ae95c3f26301}")
    private String expectedAudience;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/actuator/**", "/api/auth/**", "/error").permitAll()

                        // Endpoints de trámites (Requests)
                        .requestMatchers(HttpMethod.POST, "/api/requests").hasAnyRole("Admin", "Funcionario", "Vecino")
                        .requestMatchers(HttpMethod.PUT, "/api/requests/*/status").hasAnyRole("Admin", "Funcionario")
                        .requestMatchers(HttpMethod.GET, "/api/requests/**").hasAnyRole("Admin", "Funcionario", "Vecino", "Auditor")

                        // Endpoints de catálogo (Catalog)
                        .requestMatchers(HttpMethod.GET, "/api/catalog/**").hasAnyRole("Admin", "Funcionario", "Vecino")
                        .requestMatchers(HttpMethod.POST, "/api/catalog/**").hasAnyRole("Admin", "Funcionario")
                        .requestMatchers(HttpMethod.PUT, "/api/catalog/**").hasAnyRole("Admin", "Funcionario")
                        .requestMatchers(HttpMethod.DELETE, "/api/catalog/**").hasRole("Admin")


                        // Endpoints de reportería y KPIs
                        .requestMatchers("/api/report/**").hasRole("Admin")

                        // Endpoints de auditoría
                        .requestMatchers("/api/audit/**").hasAnyRole("Admin", "Auditor")

                        // Endpoints de administración RabbitMQ
                        .requestMatchers("/api/admin/**").hasRole("Admin")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                                .decoder(jwtDecoder())
                        )
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtRoleConverter());
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        String azureJwks = (jwkSetUri != null && !jwkSetUri.isBlank())
                ? jwkSetUri
                : "https://login.microsoftonline.com/" + tenantId + "/discovery/v2.0/keys";

        NimbusJwtDecoder azureDecoder = NimbusJwtDecoder.withJwkSetUri(azureJwks).build();
        NimbusJwtDecoder localDecoder = NimbusJwtDecoder.withPublicKey(jwtKeyProvider.getPublicKey()).build();

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(expectedAudience);
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator();
        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String iss = jwt.getIssuer() != null ? jwt.getIssuer().toString() : "";
            if (iss.contains(tenantId) || iss.contains("barriodigital") || iss.contains("microsoftonline.com") || iss.contains("windows.net")) {
                return OAuth2TokenValidatorResult.success();
            }
            return OAuth2TokenValidatorResult.failure(new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Issuer no reconocido: " + iss, null));
        };

        OAuth2TokenValidator<Jwt> azureValidator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                issuerValidator,
                audienceValidator
        );

        azureDecoder.setJwtValidator(azureValidator);
        localDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestampValidator, audienceValidator));

        return token -> {
            try {
                return azureDecoder.decode(token);
            } catch (Exception e) {
                return localDecoder.decode(token);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://127.0.0.1:4200",
                "http://localhost:80",
                "http://localhost",
                "https://100.58.58.154:4200",
                "https://100.58.58.154",
                "http://100.58.58.154:4200",
                "http://100.58.58.154",
                "https://3.90.162.113:4200",
                "https://3.90.162.113",
                "https://*.amazonaws.com"
        ));
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

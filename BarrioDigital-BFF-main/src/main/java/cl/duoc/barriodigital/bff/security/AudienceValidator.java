package cl.duoc.barriodigital.bff.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience != null && (audience.contains(expectedAudience)
                || audience.stream().anyMatch(a -> a.contains(expectedAudience)
                    || expectedAudience.contains(a)
                    || a.contains("dca1a06c-9b36-484d-8909-ae95c3f26301")
                    || a.contains("barriodigital")))) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token",
                "El token no contiene la audiencia requerida: " + expectedAudience, null));
    }
}

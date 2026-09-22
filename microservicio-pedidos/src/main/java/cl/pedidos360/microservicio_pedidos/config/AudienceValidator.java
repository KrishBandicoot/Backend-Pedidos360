package cl.pedidos360.microservicio_pedidos.config;

import java.util.List;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validador estricto de Audience (Zero Trust Architecture).
 * Garantiza que el token JWT fue emitido específicamente para este backend
 * y no para otro recurso dentro del mismo Tenant de Azure Entra ID.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;
    private final OAuth2Error error = new OAuth2Error(
        "invalid_token",
        "El token no contiene la audiencia (aud) esperada para este microservicio.",
        null
    );

    public AudienceValidator(String expectedAudience) {
        this.expectedAudience = expectedAudience;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audience = jwt.getAudience();
        if (audience != null && audience.stream().anyMatch(a -> 
            a.equalsIgnoreCase(expectedAudience) ||
            a.equalsIgnoreCase("77fa1a53-496f-49d1-97d7-bac9b72d9081") ||
            a.equalsIgnoreCase("api://77fa1a53-496f-49d1-97d7-bac9b72d9081") ||
            a.equalsIgnoreCase("api://pedidos360-backend")
        )) {
            return OAuth2TokenValidatorResult.success();
        }
        return OAuth2TokenValidatorResult.failure(error);
    }
}

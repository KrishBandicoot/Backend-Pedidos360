package cl.pedidos360.microservicio_pedidos.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 * Convierte los claims de Microsoft Entra ID (Azure AD) en GrantedAuthorities de Spring Security.
 * Mapea:
 * - App Roles ('roles'): Asigna prefijo 'ROLE_' (Ej: 'Admin' -> 'ROLE_ADMIN')
 * - Delegated Scopes ('scp' o 'scope'): Asigna prefijo 'SCOPE_' (Ej: 'Pedidos.Read' -> 'SCOPE_Pedidos.Read')
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaim = getPrincipalClaimName(jwt);
        return new JwtAuthenticationToken(jwt, authorities, principalClaim);
    }

    public Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Extraer Scopes estándar delegados (scp o scope)
        Collection<GrantedAuthority> defaultAuthorities = defaultAuthoritiesConverter.convert(jwt);
        if (defaultAuthorities != null) {
            authorities.addAll(defaultAuthorities);
        }

        // 2. Extraer Roles de Aplicación de Azure Entra ID ('roles')
        Object rolesClaim = jwt.getClaims().get("roles");
        if (rolesClaim instanceof List<?> rawRoles && !rawRoles.isEmpty()) {
            List<SimpleGrantedAuthority> roleAuthorities = rawRoles.stream()
                .filter(String.class::isInstance)
                .map(role -> {
                    String roleStr = ((String) role).trim().toUpperCase();
                    if (!roleStr.startsWith("ROLE_")) {
                        roleStr = "ROLE_" + roleStr;
                    }
                    return new SimpleGrantedAuthority(roleStr);
                })
                .collect(Collectors.toList());
            authorities.addAll(roleAuthorities);
        } else {
            // Si el Tenant de Azure AD no tiene App Roles configurados (entorno de pruebas/laboratorio),
            // se otorga acceso completo de demostración al usuario autenticado.
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return Collections.unmodifiableList(authorities);
    }

    private String getPrincipalClaimName(Jwt jwt) {
        // Entra ID suele incluir 'preferred_username', 'name' o 'sub'
        if (jwt.hasClaim("preferred_username")) {
            return jwt.getClaimAsString("preferred_username");
        }
        if (jwt.hasClaim("name")) {
            return jwt.getClaimAsString("name");
        }
        return jwt.getSubject();
    }
}

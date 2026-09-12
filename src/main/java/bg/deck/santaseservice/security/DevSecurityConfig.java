package bg.deck.santaseservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The filter chain for local development: the shared rules plus CORS.
 *
 * <p>Vite serves the app on its own port while Spring answers on another, so
 * every call the browser makes is cross-origin. The allowed origin lives in
 * {@link bg.deck.santaseservice.config.DevCorsConfig}, which is bound to this
 * same profile — so the source is always there when this chain is, and injecting
 * it directly means a misconfiguration fails at startup rather than silently
 * leaving CORS off.
 */
@Profile("dev")
@RequiredArgsConstructor
@Configuration
public class DevSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return SecurityRules.apply(http, jwtAuthenticationFilter)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .build();
    }
}

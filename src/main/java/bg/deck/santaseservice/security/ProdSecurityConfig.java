package bg.deck.santaseservice.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The filter chain for every deployed environment: the shared rules, no CORS.
 *
 * <p>nginx serves the site and proxies the API under the same origin, so the
 * browser never makes a cross-origin request and there is nothing to allow.
 *
 * <p>Bound to {@code !dev} rather than to {@code prod} on purpose. Were it named
 * after one profile, running under any third profile would match neither config,
 * leaving the application with no filter chain at all — which fails open, with
 * every endpoint unauthenticated. This way anything that is not development gets
 * the locked-down chain.
 */
@Profile("!dev")
@RequiredArgsConstructor
@Configuration
public class ProdSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return SecurityRules.apply(http, jwtAuthenticationFilter)
                .cors(AbstractHttpConfigurer::disable)
                .build();
    }
}

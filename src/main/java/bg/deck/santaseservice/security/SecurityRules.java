package bg.deck.santaseservice.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static bg.deck.santaseservice.constant.Constants.USER;

/**
 * Everything the dev and production filter chains share.
 *
 * <p>The two chains differ in exactly one respect — whether cross-origin
 * requests are allowed — and that is the only thing their configurations state.
 * The rules below decide who may reach which endpoint, so they are written once
 * here rather than copied into both: a copy that drifts is a hole in one
 * environment that does not show up in the other.
 */
final class SecurityRules {

    private SecurityRules() {
    }

    /** Stateless JWT auth and the endpoint rules, identical in every profile. */
    static HttpSecurity apply(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/game/**").hasRole(USER)
                        .requestMatchers("/tabla/**").hasRole(USER)
                        .requestMatchers("/user/confirm-deletion").permitAll()
                        .requestMatchers("/user/**").hasRole(USER)
                        .requestMatchers("/ws-game/**").hasRole(USER)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

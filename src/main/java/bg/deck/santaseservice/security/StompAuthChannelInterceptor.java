package bg.deck.santaseservice.security;

import bg.deck.santaseservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;

import static bg.deck.santaseservice.constant.Constants.BEARER;

/**
 * Authenticates the game socket on the STOMP CONNECT frame.
 *
 * <p>The socket used to be authenticated by the handshake, which meant the
 * token travelled in the URL — {@code /ws-game?token=...} — because a browser
 * cannot set headers on a WebSocket handshake. URLs are written to access
 * logs, the proxy's included, and the token being carried there was the
 * <em>refresh</em> token: the one that mints new sessions. A CONNECT frame is
 * a message, not a URL, so the header can carry the short-lived access token
 * instead and nothing is logged.
 *
 * <p>The handshake principal is still accepted when a CONNECT arrives without
 * the header, so a client running the previous build keeps working through a
 * deploy. Once those are gone, that fallback and the query-parameter branch in
 * {@link JwtAuthenticationFilter} should both go.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorise(accessor);
            default -> { /* the broker's own frames need no check */ }
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith(BEARER)) {
            // No header: a client from the previous build, authenticated by the
            // handshake. Accept it, but only if that actually happened.
            if (accessor.getUser() == null) {
                throw new BadCredentialsException("The game socket needs a token.");
            }
            return;
        }

        String jwt = header.substring(BEARER.length());
        String username = jwtService.extractUsername(jwt);

        if (username == null || !jwtService.isTokenValid(jwt)) {
            throw new BadCredentialsException("The game socket's token is not valid.");
        }

        accessor.setUser(new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(jwtService.extractRole(jwt)))));
    }

    /**
     * A player may only listen to their own topics.
     *
     * <p>Every destination the server publishes to ends with the username it is
     * meant for — {@code /topic/game/{id}/{username}} and the search topics —
     * and each carries that player's own view, their cards included. Nothing
     * stopped a signed-in player from subscribing to someone else's.
     */
    private void authorise(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        String destination = accessor.getDestination();

        if (user == null) {
            throw new BadCredentialsException("Not connected.");
        }
        if (destination == null || !destination.endsWith("/" + user.getName())) {
            log.warn("{} tried to subscribe to {}", user.getName(), destination);
            throw new AccessDeniedException("That topic belongs to another player.");
        }
    }
}

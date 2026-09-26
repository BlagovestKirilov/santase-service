package bg.deck.config;

import bg.deck.constant.LogConstants;
import bg.deck.service.AvailabilityService;
import bg.deck.util.AuthenticatedUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Stops a request to a game that is not being offered to whoever is asking.
 *
 * <p>Answers <b>404</b> rather than 403. A game somebody may not have should
 * not announce that it exists: a beta before its time is not a locked door with
 * a label on it, it is simply not there.
 *
 * <p>This is the half that counts. Leaving a card out of the lobby hides a game
 * from someone who was looking at the lobby; it does nothing about a phone
 * carrying a build from three weeks ago, a tab left open overnight, or a
 * request typed by hand.
 */
@Log4j2
@RequiredArgsConstructor
@Component
public class AvailableServiceInterceptor implements HandlerInterceptor {

    private final AvailabilityService availabilityService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }

        RequiresService required = method.getMethodAnnotation(RequiresService.class);
        if (required == null) {
            required = method.getBeanType().getAnnotation(RequiresService.class);
        }
        if (required == null) {
            return true;
        }

        String username = AuthenticatedUser.username();
        if (availabilityService.isAvailable(required.value(), username)) {
            return true;
        }

        log.info(LogConstants.SERVICE_NOT_AVAILABLE, required.value(), username, request.getRequestURI());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }
}

package bg.deck;

import bg.deck.config.AvailableServiceInterceptor;
import bg.deck.config.RequiresService;
import bg.deck.controller.GameController;
import bg.deck.controller.TablaController;
import bg.deck.service.AvailabilityService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The half that actually turns a game off.
 *
 * <p>Leaving a card out of the lobby hides a game from whoever is looking at
 * the lobby. This is what answers the phone still carrying last month's build.
 */
@DisplayName("Refusing a game that is not on offer")
class AvailableServiceInterceptorTest {

    private final AvailabilityService availabilityService = mock(AvailabilityService.class);
    private final AvailableServiceInterceptor interceptor = new AvailableServiceInterceptor(availabilityService);

    @BeforeEach
    void signIn() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("petko91", null, java.util.List.of()));
    }

    @AfterEach
    void signOut() {
        SecurityContextHolder.clearContext();
    }

    private static HandlerMethod endpoint(Class<?> controller, String method, Class<?>... parameters)
            throws NoSuchMethodException {
        return new HandlerMethod(mock(controller), controller.getMethod(method, parameters));
    }

    @Test
    @DisplayName("a game on offer goes through")
    void onOfferPassesThrough() throws Exception {
        when(availabilityService.isAvailable("TABLA", "petko91")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean carryOn = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/tabla/search"), response, endpoint(TablaController.class, "search"));

        assertTrue(carryOn);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
    }

    @Test
    @DisplayName("one that is not answers 404, not 403")
    void notOnOfferIsNotFound() throws Exception {
        when(availabilityService.isAvailable("TABLA", "petko91")).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean carryOn = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/tabla/search"), response, endpoint(TablaController.class, "search"));

        assertFalse(carryOn, "the handler is never reached");
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus(),
                "403 would tell a stranger the game exists and is being kept from them");
    }

    @Test
    @DisplayName("сантасе is gated by its own code, not табла's")
    void eachGameByItsOwnCode() throws Exception {
        when(availabilityService.isAvailable("SANTASE", "petko91")).thenReturn(false);
        when(availabilityService.isAvailable("TABLA", "petko91")).thenReturn(true);
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean carryOn = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/game/search"), response,
                endpoint(GameController.class, "searchGame"));

        assertFalse(carryOn);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    @DisplayName("an endpoint with no annotation is never asked about")
    void unannotatedEndpointsAreLeftAlone() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean carryOn = interceptor.preHandle(
                new MockHttpServletRequest("POST", "/game/play-card"), response,
                endpoint(GameController.class, "playCard", bg.deck.model.request.CardRequest.class));

        assertTrue(carryOn, "playing a card is not gated: a table already in play finishes");
        verifyNoInteractions(availabilityService);
    }

    @Test
    @DisplayName("and neither is anything that is not a handler method")
    void nonHandlersPassThrough() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(new MockHttpServletRequest("GET", "/"), response, new Object()));
        verifyNoInteractions(availabilityService);
    }

    @Test
    @DisplayName("the two search endpoints carry the annotation")
    void theSearchEndpointsAreAnnotated() throws Exception {
        assertEquals("SANTASE",
                GameController.class.getMethod("searchGame").getAnnotation(RequiresService.class).value());
        assertEquals("TABLA",
                TablaController.class.getMethod("search").getAnnotation(RequiresService.class).value());
    }

    @Test
    @DisplayName("and the endpoints that keep a game going do not")
    void movesAreNotAnnotated() throws Exception {
        for (Method method : GameController.class.getMethods()) {
            if (method.getName().equals("searchGame")) {
                continue;
            }
            assertEquals(null, method.getAnnotation(RequiresService.class),
                    method.getName() + " is gated, so switching сантасе off would cut a game in half");
        }
    }
}

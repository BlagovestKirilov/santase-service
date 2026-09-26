package bg.deck;

import bg.deck.config.AvailableServiceInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * That the gate is actually on the road.
 *
 * <p>The unit tests prove the interceptor refuses when it is asked, and they
 * would go on passing while nothing ever asked it. This boots the real context
 * and reads the chain Spring has built for {@code POST /tabla/search} — the one
 * thing that was in doubt when a public account could still start a табла game.
 *
 * <p>No request is sent: a search that got through would put a real account
 * into matchmaking against the database this runs on.
 */
@DisplayName("The gate is in the request path")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ServiceGateIntegrationTest {

    // Actuator contributes a handler mapping of its own; this is the one
    // ordinary requests go through.
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    @DisplayName("табла's search goes through the interceptor")
    void tablaSearchIsIntercepted() throws Exception {
        assertTrue(interceptsPost("/tabla/search"),
                "the annotation refuses nothing if the interceptor is not in the chain");
    }

    @Test
    @DisplayName("and сантасе's does too")
    void santaseSearchIsIntercepted() throws Exception {
        assertTrue(interceptsPost("/game/search"));
    }

    private boolean interceptsPost(String path) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        HandlerExecutionChain chain = handlerMapping.getHandler(request);

        assertNotNull(chain, path + " is not mapped at all");
        for (HandlerInterceptor interceptor : chain.getInterceptorList()) {
            if (interceptor instanceof AvailableServiceInterceptor) {
                return true;
            }
        }
        return false;
    }
}

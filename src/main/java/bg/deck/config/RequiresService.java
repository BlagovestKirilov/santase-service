package bg.deck.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an endpoint that only exists while its game is being offered.
 *
 * <p>Put it on the places a game <em>starts</em> — searching for a table — and
 * not on the moves, so that switching a game off stops new games while the ones
 * already being played finish.
 *
 * <p>Enforced by {@link AvailableServiceInterceptor}, which answers 404.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresService {

    /** The code in {@code available_service}: SANTASE, TABLA, BELOT. */
    String value();
}

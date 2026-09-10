package bg.deck.santaseservice.tabla.engine;

/**
 * One checker played with both dice in a single tap.
 *
 * <p>The board still records two ordinary {@link Hop}s — this only describes the
 * pair so the client can offer the far destination directly instead of making
 * the player aim the same checker twice. {@code via} is the intermediate point,
 * which must itself be a legal landing square: a combo is never a way around a
 * blocked midpoint.
 */
public record ComboHop(int from, int via, int to, int firstDie, int secondDie) {
}

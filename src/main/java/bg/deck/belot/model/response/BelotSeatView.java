package bg.deck.belot.model.response;

import bg.deck.belot.engine.Seat;
import bg.deck.belot.engine.Team;

/**
 * One place at the table, as everyone may see it.
 *
 * <p>{@code cardsLeft} is how many cards they still hold, which everyone at
 * a real table can see by looking. Which cards those are is not here.
 *
 * @param seat      where it is
 * @param team      which pair it belongs to
 * @param username  who is sitting there
 * @param cardsLeft how many cards they are still holding
 */
public record BelotSeatView(Seat seat, Team team, String username, int cardsLeft) {
}

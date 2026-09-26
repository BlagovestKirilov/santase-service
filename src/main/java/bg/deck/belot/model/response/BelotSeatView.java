package bg.deck.belot.model.response;

import bg.deck.belot.engine.Seat;
import bg.deck.belot.engine.Team;

/**
 * One place at the table, as everyone may see it.
 *
 * @param seat     where it is
 * @param team     which pair it belongs to
 * @param username who is sitting there
 */
public record BelotSeatView(Seat seat, Team team, String username) {
}

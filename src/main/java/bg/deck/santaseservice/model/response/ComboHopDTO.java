package bg.deck.santaseservice.model.response;

import bg.deck.santaseservice.tabla.engine.ComboHop;

/**
 * One checker's move using both dice, offered as a single destination.
 *
 * <p>The client plays it as the two hops it really is — {@code from}/{@code
 * firstDie} then {@code via}/{@code secondDie} — so undo still steps back one
 * die at a time and the server needs no new endpoint.
 *
 * @param via the intermediate point, itself a legal landing square
 */
public record ComboHopDTO(int from, int via, int to, int firstDie, int secondDie) {
    public static ComboHopDTO from(ComboHop hop) {
        return new ComboHopDTO(hop.from(), hop.via(), hop.to(), hop.firstDie(), hop.secondDie());
    }
}

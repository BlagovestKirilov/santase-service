package bg.deck.belot.engine;

import java.util.Collection;

/**
 * What cards are worth in a given contract.
 *
 * <p>The ten for the last trick and the ninety for capot are the deal's, not a
 * card's, so they are added by the scorer and not here. RULES §3.
 */
public final class CardPoints {

    /** The last trick, to whoever takes it. */
    public static final int LAST_TRICK = 10;
    /** Every trick to one team. */
    public static final int CAPOT = 90;

    private CardPoints() {
    }

    public static int of(Card card, Contract contract) {
        return card.rank().points(contract.isTrump(card.suit()));
    }

    public static int of(Collection<Card> cards, Contract contract) {
        return cards.stream().mapToInt(card -> of(card, contract)).sum();
    }

    /** Every card in the deck, plus the last trick: what one deal is worth. */
    public static int dealTotal(Contract contract) {
        return of(Deck.full(), contract) + LAST_TRICK;
    }
}

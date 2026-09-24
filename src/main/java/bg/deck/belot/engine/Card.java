package bg.deck.belot.engine;

/**
 * One of the 32 cards.
 *
 * @param suit its suit
 * @param rank its rank
 */
public record Card(Suit suit, Rank rank) {

    @Override
    public String toString() {
        return rank.symbol() + suit.name().charAt(0);
    }
}

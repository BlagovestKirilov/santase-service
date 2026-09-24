package bg.deck.belot.engine;

/**
 * Something a hand holds and says out loud on its first trick.
 *
 * @param kind    what it is
 * @param suit    the suit it is in — a sequence's or a belote's; null for a carré
 * @param topRank the highest card of a sequence, or the rank of a carré
 * @param points  what it is worth
 */
public record Declaration(DeclarationKind kind, Suit suit, Rank topRank, int points) {

    /** Sequences are measured by length first, then by their top card. */
    public int length() {
        return switch (kind) {
            case TERZ -> 3;
            case QUARTE -> 4;
            case QUINTE -> 5;
            default -> 0;
        };
    }

    public boolean isSequence() {
        return length() > 0;
    }
}

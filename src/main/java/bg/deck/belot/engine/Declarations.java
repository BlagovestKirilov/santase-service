package bg.deck.belot.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * What a hand holds worth declaring. RULES §7.
 *
 * <p>Sequences run by the <em>natural</em> order of the cards — 7 8 9 10 J Q K A
 * — and not by the trump order that decides tricks. A jack sits between the ten
 * and the queen here however mighty it is in play.
 *
 * <p>Only the longest run in a suit counts: four in a row is one quarte, never
 * two overlapping terzes.
 */
public final class Declarations {

    /** Shortest run worth saying. */
    private static final int SHORTEST_SEQUENCE = 3;

    private Declarations() {
    }

    /**
     * Everything in this hand that may be declared under this contract.
     *
     * <p>Nothing at all in no trumps: "играчите нямат право да обявяват
     * притежаваните от тях комбинации".
     */
    public static List<Declaration> in(List<Card> hand, Contract contract) {
        if (contract == Contract.NO_TRUMPS) {
            return List.of();
        }
        List<Declaration> found = new ArrayList<>(sequences(hand));
        found.addAll(carres(hand));
        found.addAll(belotes(hand, contract));
        return List.copyOf(found);
    }

    /** The longest run of three or more in each suit. */
    static List<Declaration> sequences(List<Card> hand) {
        List<Declaration> found = new ArrayList<>();
        Map<Suit, List<Rank>> bySuit = hand.stream()
                .collect(Collectors.groupingBy(Card::suit,
                        Collectors.mapping(Card::rank, Collectors.toList())));

        bySuit.forEach((suit, ranks) -> {
            List<Rank> sorted = ranks.stream().sorted(Comparator.comparingInt(Rank::naturalOrder)).toList();
            int runStart = 0;
            for (int i = 1; i <= sorted.size(); i++) {
                boolean consecutive = i < sorted.size()
                        && sorted.get(i).naturalOrder() == sorted.get(i - 1).naturalOrder() + 1;
                if (!consecutive) {
                    int length = i - runStart;
                    if (length >= SHORTEST_SEQUENCE) {
                        found.add(sequence(suit, sorted.get(i - 1), length));
                    }
                    runStart = i;
                }
            }
        });
        return found;
    }

    private static Declaration sequence(Suit suit, Rank top, int length) {
        DeclarationKind kind = switch (length) {
            case 3 -> DeclarationKind.TERZ;
            case 4 -> DeclarationKind.QUARTE;
            // OPEN 14 — six, seven and eight in a row are scored as a quinte
            // here. The rules page stops at five.
            default -> DeclarationKind.QUINTE;
        };
        return new Declaration(kind, suit, top, kind.points());
    }

    /** Four of a kind, where the rank is worth anything. */
    static List<Declaration> carres(List<Card> hand) {
        return hand.stream()
                .collect(Collectors.groupingBy(Card::rank, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() == 4)
                .map(entry -> new Declaration(DeclarationKind.CARRE, null, entry.getKey(), carrePoints(entry.getKey())))
                .filter(declaration -> declaration.points() > 0)
                .sorted(Comparator.comparingInt(declaration -> -declaration.points()))
                .toList();
    }

    private static int carrePoints(Rank rank) {
        return switch (rank) {
            case JACK -> 200;
            case NINE -> 150;
            case ACE, TEN, KING, QUEEN -> 100;
            // Four sevens or four eights are worth saying nothing about.
            case EIGHT, SEVEN -> 0;
        };
    }

    /**
     * King and queen together in a trump suit.
     *
     * <p>OPEN 15 — in all trumps every suit is a trump suit, so a belote is
     * counted in each suit that holds both. If the answer is that all trumps has
     * no belote at all, or only one, this is the method that changes.
     */
    static List<Declaration> belotes(List<Card> hand, Contract contract) {
        return java.util.Arrays.stream(Suit.values())
                .filter(contract::isTrump)
                .filter(suit -> hand.contains(new Card(suit, Rank.KING))
                        && hand.contains(new Card(suit, Rank.QUEEN)))
                .map(suit -> new Declaration(DeclarationKind.BELOTE, suit, Rank.KING, DeclarationKind.BELOTE.points()))
                .toList();
    }
}

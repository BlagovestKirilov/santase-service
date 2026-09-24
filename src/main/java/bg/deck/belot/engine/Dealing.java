package bg.deck.belot.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Handing out the cards. RULES §4.
 *
 * <p>Three each, then two, then — once the bidding is done — three more. The
 * packets are dealt in that order because that is how the table sees it; the
 * last three are already decided by the shuffle, so a hand can be built whole
 * and its first five shown to the bidding.
 */
public final class Dealing {

    /** How many each player holds once the bidding is over. */
    public static final int HAND_SIZE = 8;
    /** What they see while they bid. */
    public static final int BEFORE_BIDDING = 5;

    private static final int[] PACKETS = {3, 2, 3};

    private Dealing() {
    }

    public static List<Card> shuffled(Random random) {
        List<Card> deck = new ArrayList<>(Deck.full());
        Collections.shuffle(deck, random);
        return deck;
    }

    /** The four hands, dealt counter-clockwise from the dealer's right. */
    public static Map<Seat, List<Card>> deal(List<Card> shuffled, Seat dealer) {
        Map<Seat, List<Card>> hands = new EnumMap<>(Seat.class);
        for (Seat seat : Seat.values()) {
            hands.put(seat, new ArrayList<>(HAND_SIZE));
        }

        int next = 0;
        for (int packet : PACKETS) {
            Seat seat = dealer.next();
            for (int player = 0; player < Seat.values().length; player++) {
                hands.get(seat).addAll(shuffled.subList(next, next + packet));
                next += packet;
                seat = seat.next();
            }
        }
        return hands;
    }

    /** What a hand looks like while its owner is bidding. */
    public static List<Card> beforeBidding(List<Card> hand) {
        return List.copyOf(hand.subList(0, BEFORE_BIDDING));
    }
}

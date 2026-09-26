package bg.deck.belot.model;

import bg.deck.belot.engine.Bidding;
import bg.deck.belot.engine.Contract;
import bg.deck.belot.engine.Doubling;
import bg.deck.belot.engine.Seat;
import bg.deck.model.base.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * One hand of belot: its number, who dealt it, and what was said over it.
 *
 * <p>The cards are not here. {@code dealNumber} and the game's seed reproduce
 * the shuffle, so a hand is derived whenever it is needed and cannot drift from
 * what the table was promised before the first card.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(schema = "belot", name = "deal")
public class BelotDeal extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "game_id", nullable = false)
    private BelotGame game;

    /** Counted from one, and the number the shuffle is derived from. */
    @Column(name = "deal_number", nullable = false)
    private int dealNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "dealer_seat", nullable = false, length = 10)
    private Seat dealerSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BelotDealStatus status = BelotDealStatus.BIDDING;

    /** What was bid, once the bidding is over. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "declarer_seat", length = 10)
    private Seat declarerSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Doubling doubling = Doubling.NONE;

    @OrderBy("ordinal ASC")
    @OneToMany(mappedBy = "deal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BelotBid> bids = new ArrayList<>();

    public BelotDeal(BelotGame game, int dealNumber, Seat dealerSeat) {
        this.game = game;
        this.dealNumber = dealNumber;
        this.dealerSeat = dealerSeat;
    }

    /**
     * The bidding as it stands, replayed from the turns that were taken.
     *
     * <p>Rebuilt rather than stored: {@link Bidding} already knows every rule
     * about whose turn it is and what beats what, and a second copy of that
     * knowledge in columns is a second copy to get wrong.
     */
    public Bidding bidding() {
        Bidding bidding = Bidding.startedBy(dealerSeat);
        for (BelotBid bid : bids) {
            bidding = bidding.apply(bid.action());
        }
        return bidding;
    }

    public void add(BelotBid bid) {
        bid.setDeal(this);
        bids.add(bid);
    }

    public int nextOrdinal() {
        return bids.size();
    }
}

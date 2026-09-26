package bg.deck.belot.service;

import bg.deck.belot.engine.BidAction;
import bg.deck.belot.engine.Bidding;
import bg.deck.belot.engine.Card;
import bg.deck.belot.engine.Seat;
import bg.deck.belot.model.BelotDeal;
import bg.deck.belot.model.BelotDealStatus;
import bg.deck.belot.model.BelotGame;
import bg.deck.belot.model.BelotSeat;
import bg.deck.belot.model.request.BelotBidRequest;
import bg.deck.belot.model.response.BelotBidView;
import bg.deck.belot.model.response.BelotBiddingView;
import bg.deck.belot.model.response.BelotSeatView;
import bg.deck.belot.model.response.BelotStateResponse;
import bg.deck.enums.GameType;
import bg.deck.service.AvailabilityService;
import bg.deck.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * What a player asks of belot: sit down, look at the table, say something.
 *
 * <p>Holds no repository of its own. The table is {@link BelotTableService}'s,
 * the deal is {@link BelotDealService}'s, and this arranges them in the order a
 * turn happens: change something, then tell all four what the table looks like
 * from where they are sitting.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class BelotService {

    /** The catalogue code. Not a {@code GameType}: belot keeps out of that enum. */
    public static final String BELOT = "BELOT";

    private final BelotTableService belotTableService;
    private final BelotDealService belotDealService;
    private final BelotPlayerService belotPlayerService;
    private final AvailabilityService availabilityService;
    private final WebSocketService webSocketService;

    /**
     * Sits a player down, and deals if that filled the table.
     *
     * <p>Gated like the other two searches: nobody joins a queue for a game
     * they are not being offered. {@link GameType} is deliberately not involved
     * — belot is not in that enum, by the rule in {@code docs/belot/BUILD.md}.
     */
    @Transactional
    public void search(String username) {
        availabilityService.requireAvailable(BELOT, username);
        belotPlayerService.ensureKnown(username);

        BelotGame table = belotTableService.join(username);
        if (table.isFull() && belotDealService.current(table).isEmpty()) {
            belotDealService.dealNext(table);
        }

        tellEveryone(table);
    }

    /** Sends this player their own view again — a reload, or a reconnect. */
    @Transactional(readOnly = true)
    public void sendState(String username) {
        belotTableService.tableOf(username).ifPresent(table -> tell(table, username));
    }

    /**
     * One turn of the bidding, from whoever is calling.
     *
     * <p>The seat comes from the table, never from the request: a request that
     * could name a seat could name somebody else's.
     */
    @Transactional
    public void bid(String username, BelotBidRequest request) {
        BelotGame table = belotTableService.tableOf(username).orElseThrow(
                () -> new IllegalStateException(username + " is not at a belot table"));
        BelotSeat seat = table.seatOf(username).orElseThrow(
                () -> new IllegalStateException(username + " has no seat at table " + table.getId()));
        BelotDeal deal = belotDealService.current(table).orElseThrow(
                () -> new IllegalStateException("No deal in progress at table " + table.getId()));

        belotDealService.bid(deal, new BidAction(seat.getSeat(), request.kind(), request.contract()));

        // Nobody wanted it: the next seat deals, and the table is told once.
        if (deal.getStatus() == BelotDealStatus.THROWN_IN) {
            belotDealService.dealNext(table);
        }

        tellEveryone(table);
    }

    private void tellEveryone(BelotGame table) {
        table.getSeats().forEach(seat -> tell(table, seat.getUsername()));
    }

    private void tell(BelotGame table, String username) {
        webSocketService.notifyBelotUpdate(table.getId().toString(), username, viewFor(table, username));
    }

    /** The table as one seat sees it, with that seat's hand and nobody else's. */
    private BelotStateResponse viewFor(BelotGame table, String username) {
        Seat seat = table.seatOf(username).map(BelotSeat::getSeat).orElse(null);
        Optional<BelotDeal> deal = belotDealService.current(table);

        List<Card> hand = deal.isPresent() && seat != null
                ? belotDealService.visibleHand(table, deal.get(), seat)
                : List.of();

        return new BelotStateResponse(
                table.getId(),
                table.getStatus(),
                table.getServerSeedHash(),
                table.getSeats().stream()
                        .map(taken -> new BelotSeatView(taken.getSeat(), taken.team(), taken.getUsername()))
                        .toList(),
                seat,
                deal.map(BelotDeal::getDealNumber).orElse(null),
                deal.map(BelotDeal::getDealerSeat).orElse(null),
                deal.map(BelotDeal::getStatus).orElse(null),
                hand,
                deal.map(current -> biddingFor(current, seat)).orElse(null),
                table.getNorthSouthScore(),
                table.getEastWestScore(),
                table.getHangingPoints());
    }

    /**
     * The bidding, with the calls this seat may make now and no others.
     *
     * <p>Sending every seat its own legal moves rather than the rules is what
     * keeps the client from having to know them — and from offering a button
     * the server would refuse.
     */
    private BelotBiddingView biddingFor(BelotDeal deal, Seat seat) {
        if (deal.getStatus() != BelotDealStatus.BIDDING) {
            return null;
        }
        Bidding bidding = deal.bidding();

        List<BelotBidView> yours = bidding.toAct() == seat
                ? bidding.legalActions().stream().map(BelotBidView::of).toList()
                : List.of();

        return new BelotBiddingView(
                bidding.toAct(),
                bidding.highestBid(),
                bidding.bidder(),
                bidding.doubling(),
                deal.getBids().stream().map(bid -> BelotBidView.of(bid.action())).toList(),
                yours);
    }
}

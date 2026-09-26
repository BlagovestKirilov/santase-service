package bg.deck.belot.model;

/** Where a deal has got to. */
public enum BelotDealStatus {

    /** Cards are out, the table is naming a contract. */
    BIDDING,
    /** A contract was named; the tricks are being played. */
    PLAYING,
    /** All four passed. Nothing is scored and the next seat deals. */
    THROWN_IN,
    /** Played out and scored. */
    FINISHED
}

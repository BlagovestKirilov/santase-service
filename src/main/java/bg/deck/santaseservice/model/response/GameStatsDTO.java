package bg.deck.santaseservice.model.response;

/**
 * One game's record for a player.
 *
 * @param placementGamesRemaining games still needed before a rank is assigned.
 *                                Sent by the server so the client stops
 *                                duplicating the placement threshold.
 */
public record GameStatsDTO(
        int wins,
        int losses,
        int rating,
        String rank,
        int placementGamesRemaining
) {
}

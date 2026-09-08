package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.RankingConstants;
import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.UserGameStats;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    @Transactional
    public void updateRankingAfterGame(Game game) {

        Player winner = game.getWinner();
        Player loser = game.getOpponent(winner);

        // Rating is per game type: Santase results never move a табла rating.
        UserGameStats winnerStats = winner.getUser().statsFor(game.getGameType());
        UserGameStats loserStats = loser.getUser().statsFor(game.getGameType());

        int winnerDelta = calculateEloDelta(winnerStats.getRating(),
                loserStats.getRating(), true, winnerStats.totalGames());

        int loserDelta = calculateEloDelta(loserStats.getRating(),
                winnerStats.getRating(), false, loserStats.totalGames());

        winnerStats.setRating(winnerStats.getRating() + winnerDelta);
        loserStats.setRating(loserStats.getRating() + loserDelta);

        winnerStats.setRank(resolveRank(winnerStats));
        loserStats.setRank(resolveRank(loserStats));
    }

    private int calculateEloDelta(int playerRating, int opponentRating, boolean win, int gamesPlayed) {
        int kFactor = gamesPlayed < RankingConstants.PLACEMENT_GAMES ?
                RankingConstants.K_PLACEMENT : RankingConstants.K_RANKED;

        double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));

        int result = win ? 1 : 0;

        return (int) Math.round(kFactor * (result - expected));
    }

    private Rank resolveRank(UserGameStats stats) {

        int gamesPlayed = stats.totalGames();
        int rating = stats.getRating();

        if (gamesPlayed < RankingConstants.PLACEMENT_GAMES) {
            return Rank.UNRANKED;
        }

        if (rating < RankingConstants.BRONZE_THRESHOLD) return Rank.BRONZE;
        if (rating < RankingConstants.SILVER_THRESHOLD) return Rank.SILVER;
        if (rating < RankingConstants.GOLD_THRESHOLD) return Rank.GOLD;
        if (rating < RankingConstants.PLATINUM_THRESHOLD) return Rank.PLATINUM;
        if (rating < RankingConstants.DIAMOND_THRESHOLD) return Rank.DIAMOND;

        return Rank.LEGEND;
    }
}

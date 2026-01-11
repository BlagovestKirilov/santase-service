package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.RankingConstants;
import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.model.Game;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    @Transactional
    public void updateRankingAfterGame(Game game) {

        Player winner = game.getWinner();
        Player loser = game.getOpponent(winner);

        User winnerUser = winner.getUser();
        User loserUser = loser.getUser();

        int winnerGames = totalGames(winnerUser);
        int loserGames = totalGames(loserUser);

        int winnerDelta = calculateEloDelta(winnerUser.getRankRating(),
                loserUser.getRankRating(), true, winnerGames);

        int loserDelta = calculateEloDelta(loserUser.getRankRating(),
                winnerUser.getRankRating(), false, loserGames);

        winnerUser.setRankRating(winnerUser.getRankRating() + winnerDelta);
        loserUser.setRankRating(loserUser.getRankRating() + loserDelta);

        winnerUser.setRank(resolveRank(winnerUser));
        loserUser.setRank(resolveRank(loserUser));
    }

    private int calculateEloDelta(int playerRating, int opponentRating, boolean win, int gamesPlayed) {
        int kFactor = gamesPlayed < RankingConstants.PLACEMENT_GAMES ?
                RankingConstants.K_PLACEMENT : RankingConstants.K_RANKED;

        double expected = 1.0 / (1.0 + Math.pow(10, (opponentRating - playerRating) / 400.0));

        int result = win ? 1 : 0;

        return (int) Math.round(kFactor * (result - expected));
    }

    private Rank resolveRank(User user) {

        int gamesPlayed = totalGames(user);
        int rating = user.getRankRating();

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

    private int totalGames(User user) {
        return user.getSantaseWins() + user.getSantaseLosses();
    }
}

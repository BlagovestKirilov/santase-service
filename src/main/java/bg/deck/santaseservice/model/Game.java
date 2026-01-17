package bg.deck.santaseservice.model;

import bg.deck.santaseservice.exception.UserNotPartOfGameException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Game extends BaseEntity {

    @ManyToOne
    private Player firstPlayer;

    @ManyToOne
    private Player secondPlayer;

    @OneToOne(cascade = CascadeType.ALL)
    private GameState state;

    @ManyToOne
    private Player winner;

    @ManyToOne
    private Player leftPlayer;

    private Instant finishedAt;

    public Player getPlayerByUsername(String username) {
        if (username.equals(firstPlayer.getUsername())) {
            return firstPlayer;
        } else if (username.equals(secondPlayer.getUsername())) {
            return secondPlayer;
        } else {
            throw new UserNotPartOfGameException(username);
        }
    }

    public Player getOpponentPlayerByUsername(String username) {
        if (username.equals(firstPlayer.getUsername())) {
            return secondPlayer;
        } else if (username.equals(secondPlayer.getUsername())) {
            return firstPlayer;
        } else {
            throw new UserNotPartOfGameException(username);
        }
    }

    public Player getOpponent(Player player) {
        if (player.equals(firstPlayer)) {
            return secondPlayer;
        } else if (player.equals(secondPlayer)) {
            return firstPlayer;
        } else {
            throw new UserNotPartOfGameException(player.getUsername());
        }
    }

    public void setWinner(Player winnerPlayer, boolean opponentLeft) {
        this.winner = winnerPlayer;
        this.finishedAt = Instant.now();

        Player opponent = getOpponent(winnerPlayer);
        if (opponentLeft) {
            this.leftPlayer = opponent;
        }

        winnerPlayer.getUser().incrementSantaseWins();
        opponent.getUser().incrementSantaseLosses();
    }
}

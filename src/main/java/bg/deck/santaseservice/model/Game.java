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

    public void setWinner(Player winnerPlayer) {
        this.winner = winnerPlayer;

        winnerPlayer.getUser().incrementSantaseWins();
        getOpponent(winnerPlayer).getUser().incrementSantaseLosses();
    }
}

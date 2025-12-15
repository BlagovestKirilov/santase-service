package com.bussiness.santaseservice.model;

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
            throw new RuntimeException("Player is not part of the game");
        }
    }

    public Player getOpponent(Player player) {
        if (player.equals(firstPlayer)) {
            return secondPlayer;
        } else if (player.equals(secondPlayer)) {
            return firstPlayer;
        } else {
            throw new RuntimeException("Player is not part of the game");
        }
    }
}

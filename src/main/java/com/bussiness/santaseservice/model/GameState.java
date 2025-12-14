package com.bussiness.santaseservice.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GameState extends BaseEntity {

    @ElementCollection
    @CollectionTable(name = "game_deck",
            joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> deck;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "trump_card_uuid")),
            @AttributeOverride(name = "suit", column = @Column(name = "trump_card_suit")),
            @AttributeOverride(name = "rank", column = @Column(name = "trump_card_rank"))
    })
    private Card trumpCard;

    @ElementCollection
    @CollectionTable(name = "game_first_player_hand",
            joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> firstPlayerHand;

    @ElementCollection
    @CollectionTable(name = "game_second_player_hand",
            joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> secondPlayerHand;

    private Integer firstPlayerScore;

    private Integer secondPlayerScore;

    private String firstTurnPlayerUsername;

    private String inTurnPlayerUsername;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "first_player_played_uuid")),
            @AttributeOverride(name = "suit", column = @Column(name = "first_player_played_suit")),
            @AttributeOverride(name = "rank", column = @Column(name = "first_player_played_rank"))
    })
    private Card firstPlayerPlayedCard;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "id", column = @Column(name = "second_player_played_uuid")),
            @AttributeOverride(name = "suit", column = @Column(name = "second_player_played_suit")),
            @AttributeOverride(name = "rank", column = @Column(name = "second_player_played_rank"))
    })
    private Card secondPlayerPlayedCard;

    private String closedByUsername;

    private Boolean isFirstPlayerBlanked;

    private Boolean isSecondPlayerBlanked;

    public boolean isClosed() {
        return this.closedByUsername != null;
    }
}

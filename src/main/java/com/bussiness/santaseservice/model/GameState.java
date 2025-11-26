package com.bussiness.santaseservice.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@Entity
public class GameState extends BaseEntity{

    @ElementCollection
    @CollectionTable(name = "game_deck",
            joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> deck = new ArrayList<>();

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
    private List<Card> firstPlayerHand = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "game_second_player_hand",
            joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> secondPlayerHand = new ArrayList<>();

    private Integer firstPlayerScore = 0;

    private Integer secondPlayerScore = 0;

    private Boolean isPlayer1Turn = Boolean.TRUE;

    //public boolean closed = false;

    private String winner;

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
}

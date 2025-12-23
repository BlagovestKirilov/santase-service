package com.bussiness.santaseservice.model.response;

import com.bussiness.santaseservice.model.dto.CardDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameStateResponse {
    private List<CardDTO> deck;
    private CardDTO trumpCard;
    private CardDTO playedCard;
    private CardDTO opponentPlayedCard;
    private int opponentPlayerCardsCount;
    private int remainingCardsCount;
    private String firstPlayerUsername;
    private int firstPlayerResult;
    private String secondPlayerUsername;
    private int secondPlayerResult;
    @JsonProperty("isOnTurn")
    private boolean isOnTurn;
    @JsonProperty("isClosed")
    private boolean isClosed;
    private String winnerUsername;
    private String trickWinnerUsername;
    private int trickFirstPlayerScore;
    private int trickSecondPlayerScore;
    private Integer bonus;
    private Integer opponentPlayerBonus;
}

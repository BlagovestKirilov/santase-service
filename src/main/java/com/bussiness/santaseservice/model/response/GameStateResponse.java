package com.bussiness.santaseservice.model.response;

import com.bussiness.santaseservice.model.dto.CardDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GameStateResponse {
    private UUID gameId;
    private List<CardDTO> deck;
    private CardDTO trumpCard;
    private CardDTO playedCard;
    private CardDTO opponentPlayedCard;
    private int remainingCardsCount;
    private String firstPlayerUsername;
    private int firstPlayerResult;
    private String secondPlayerUsername;
    private int secondPlayerResult;
    @JsonProperty("isOnTurn")
    private boolean isOnTurn;
}

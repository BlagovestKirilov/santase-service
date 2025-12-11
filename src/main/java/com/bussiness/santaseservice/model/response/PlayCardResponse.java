package com.bussiness.santaseservice.model.response;

import com.bussiness.santaseservice.model.Card;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
public class PlayCardResponse {
    private UUID gameId;
    private List<Card> deck;
}

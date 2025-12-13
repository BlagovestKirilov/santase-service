package com.bussiness.santaseservice.model.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class CardDTO {
    private UUID id;
    private String suit;
    private String rank;
    private int points;
}

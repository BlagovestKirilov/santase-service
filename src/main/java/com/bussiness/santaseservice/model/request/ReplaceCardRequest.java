package com.bussiness.santaseservice.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class ReplaceCardRequest {
    private UUID gameId;
    private String username;
}

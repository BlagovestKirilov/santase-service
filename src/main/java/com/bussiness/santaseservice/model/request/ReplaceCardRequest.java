package com.bussiness.santaseservice.model.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class ReplaceCardRequest {
    private Long gameId;
    private String username;
}

package com.bussiness.santaseservice.model.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
@NoArgsConstructor
public class CardRequest {
    @NotNull(message = "cardId must not be null")
    private UUID cardId;
}

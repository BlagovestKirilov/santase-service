package bg.deck.santaseservice.model.request;

import bg.deck.santaseservice.annotation.ValidUUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CardRequest {
    @ValidUUID
    private UUID cardId;
}

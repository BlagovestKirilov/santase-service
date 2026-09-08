package bg.deck.santaseservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One checker move. Only the origin and the die are sent — the destination is
 * derived server-side so a client cannot desync the board.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveRequest {

    /** 1..24 for a point, 25 for the bar. */
    @NotNull(message = "Полето 'from' е задължително.")
    @Min(value = 1, message = "Невалидно поле.")
    @Max(value = 25, message = "Невалидно поле.")
    private Integer from;

    @NotNull(message = "Полето 'die' е задължително.")
    @Min(value = 1, message = "Невалиден зар.")
    @Max(value = 6, message = "Невалиден зар.")
    private Integer die;
}

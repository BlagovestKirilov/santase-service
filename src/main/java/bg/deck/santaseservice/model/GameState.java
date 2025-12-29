package bg.deck.santaseservice.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class GameState extends BaseEntity {

    @ElementCollection
    @CollectionTable(name = "game_deck", joinColumns = @JoinColumn(name = "game_id"))
    private List<Card> deck;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "trump_card_id"))
    @AttributeOverride(name = "suit", column = @Column(name = "trump_card_suit"))
    @AttributeOverride(name = "rank", column = @Column(name = "trump_card_rank"))
    private Card trumpCard;

    @ManyToOne
    private Player firstTurnPlayer;

    @ManyToOne
    private Player inTurnPlayer;

    @ManyToOne
    private Player closedByPlayer;

    public boolean isClosed() {
        return this.closedByPlayer != null;
    }
}

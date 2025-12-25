package bg.deck.santaseservice.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Setter
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Player extends BaseEntity {
    @OneToOne
    private User user;

    private Integer result;

    private Integer score;

    @Transient
    private Integer bonus;

    @ElementCollection
    @CollectionTable(name = "player_hand",
            joinColumns = @JoinColumn(name = "player_id"))
    private List<Card> hand;

    @Embedded
    @AttributeOverride(name = "id", column = @Column(name = "played_card_id"))
    @AttributeOverride(name = "suit", column = @Column(name = "played_card_suit"))
    @AttributeOverride(name = "rank", column = @Column(name = "played_card_rank"))
    private Card playedCard;

    private Boolean isBlanked;

    public String getUsername() {
        return user.getUsername();
    }
}

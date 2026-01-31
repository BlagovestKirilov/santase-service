package bg.deck.santaseservice.model.base;

import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@MappedSuperclass
public abstract class BaseUser extends BaseEntity {
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String ipAddress;

    private Integer santaseWins;

    private Integer santaseLosses;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rank rank;

    @Column(nullable = false)
    private Integer rankRating;

    @Column(nullable = false)
    private Boolean isEmailConfirmed;

    public void incrementSantaseWins() {
        this.santaseWins++;
    }

    public void incrementSantaseLosses() {
        this.santaseLosses++;
    }
}

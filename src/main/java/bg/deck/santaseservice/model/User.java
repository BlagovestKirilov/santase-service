package bg.deck.santaseservice.model;

import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email")
})
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String username;

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
    private int rankRating;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private Boolean isEmailConfirmed;

    public void incrementSantaseWins() {
        this.santaseWins++;
    }

    public void incrementSantaseLosses() {
        this.santaseLosses++;
    }
}

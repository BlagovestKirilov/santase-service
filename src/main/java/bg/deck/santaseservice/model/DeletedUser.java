package bg.deck.santaseservice.model;

import bg.deck.santaseservice.enums.Rank;
import bg.deck.santaseservice.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "deleted_users")
public class DeletedUser extends BaseEntity {
    @Column(nullable = false)
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
    private Integer rankRating;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Boolean isEmailConfirmed;

    @Column(nullable = false)
    private Instant deletedAt;
}

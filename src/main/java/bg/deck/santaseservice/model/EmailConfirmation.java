package bg.deck.santaseservice.model;

import bg.deck.santaseservice.enums.EmailConfirmationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Setter
@Getter
@ToString
@NoArgsConstructor
@Entity
public class EmailConfirmation extends BaseEntity {
    @OneToOne
    private User user;

    @Column(nullable = false)
    private String confirmationToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailConfirmationStatus status;

    @OneToOne
    private DeletedUser deletedUser;

    public EmailConfirmation(User user) {
        this.user = user;
        this.confirmationToken = UUID.randomUUID().toString();
        this.status = EmailConfirmationStatus.PENDING;
    }
}

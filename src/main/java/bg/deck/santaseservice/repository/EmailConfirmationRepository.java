package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.EmailConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, UUID> {
    Optional<EmailConfirmation> findByConfirmationToken(String confirmationToken);

    Optional<EmailConfirmation> findByUserUsername(String email);
}

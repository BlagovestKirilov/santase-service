package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.EmailConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, Integer> {
    Optional<EmailConfirmation> findByConfirmationToken(String confirmationToken);
}

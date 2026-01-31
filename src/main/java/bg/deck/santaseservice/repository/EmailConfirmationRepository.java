package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.enums.EmailConfirmationStatus;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, UUID> {
    Optional<EmailConfirmation> findByConfirmationTokenAndStatus(UUID confirmationToken, EmailConfirmationStatus status);

    List<EmailConfirmation> findAllByUserAndStatus(User user, EmailConfirmationStatus status);

    List<EmailConfirmation> findAllByUser(User user);
}

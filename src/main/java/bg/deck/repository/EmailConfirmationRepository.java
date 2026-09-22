package bg.deck.repository;

import bg.deck.enums.EmailConfirmationStatus;
import bg.deck.model.EmailConfirmation;
import bg.deck.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailConfirmationRepository extends JpaRepository<EmailConfirmation, UUID> {
    Optional<EmailConfirmation> findByConfirmationTokenAndStatus(UUID confirmationToken, EmailConfirmationStatus status);

    List<EmailConfirmation> findAllByUserAndStatus(User user, EmailConfirmationStatus status);

    List<EmailConfirmation> findAllByUser(User user);
}

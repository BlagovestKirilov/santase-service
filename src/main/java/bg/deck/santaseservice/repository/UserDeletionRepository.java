package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.enums.UserDeletionStatus;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.UserDeletion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserDeletionRepository extends JpaRepository<UserDeletion, Integer> {
    Optional<UserDeletion> findByUserDeletionTokenAndStatus(UUID token, UserDeletionStatus status);

    List<UserDeletion> findAllByUserAndStatus(User user, UserDeletionStatus status);

    List<UserDeletion> findAllByUser(User user);
}

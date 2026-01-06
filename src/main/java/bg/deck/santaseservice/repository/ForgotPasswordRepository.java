package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.enums.ForgotPasswordStatus;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    Optional<ForgotPassword> findByForgotPasswordTokenAndStatus(String token, ForgotPasswordStatus status);

    List<ForgotPassword> findAllByUserAndStatus(User user, ForgotPasswordStatus status);
}

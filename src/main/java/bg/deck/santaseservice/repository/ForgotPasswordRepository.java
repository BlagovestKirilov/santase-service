package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.ForgotPassword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ForgotPasswordRepository extends JpaRepository<ForgotPassword, UUID> {
    Optional<ForgotPassword> findByForgotPasswordToken(String token);
}

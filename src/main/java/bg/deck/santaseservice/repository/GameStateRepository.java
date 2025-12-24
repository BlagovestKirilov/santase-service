package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameStateRepository extends JpaRepository<GameState, UUID> {
}

package bg.deck.repository;

import bg.deck.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameStateRepository extends JpaRepository<GameState, UUID> {
}

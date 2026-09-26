package bg.deck.repository;

import bg.deck.model.AvailableService;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AvailableServiceRepository extends JpaRepository<AvailableService, UUID> {
}

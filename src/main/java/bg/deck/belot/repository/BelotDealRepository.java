package bg.deck.belot.repository;

import bg.deck.belot.model.BelotDeal;
import bg.deck.belot.model.BelotGame;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BelotDealRepository extends JpaRepository<BelotDeal, UUID> {

    /** The deal being played at this table, if one is. */
    Optional<BelotDeal> findFirstByGameOrderByDealNumberDesc(BelotGame game);
}

package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
    @Query("""
                 SELECT game FROM Game game
                 WHERE (game.firstPlayer.user.username = :username
                    OR game.secondPlayer.user.username = :username)
                 AND game.winner IS NULL
                 ORDER BY game.createdAt DESC
            """)
    List<Game> findActiveGamesByUsername(@Param("username") String username);
}

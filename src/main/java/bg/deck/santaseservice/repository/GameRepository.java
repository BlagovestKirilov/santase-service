package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
    @Query("""
                 SELECT g FROM Game g
                 WHERE (g.firstPlayer.user.username = :username
                    OR g.secondPlayer.user.username = :username)
                 AND g.winner IS NULL
                 ORDER BY g.createdAt DESC
            """)
    List<Game> findActiveGamesByUsername(@Param("username") String username);
}

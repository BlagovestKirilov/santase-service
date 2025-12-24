package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    @Query("""
                SELECT game.id FROM Game game
                WHERE (game.firstPlayer.user.username = :username
                   OR game.secondPlayer.user.username = :username)
                AND game.winner IS NULL
            """)
    Optional<UUID> findActiveGameIdByUsername(@Param("username") String username);
}

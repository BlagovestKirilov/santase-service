package com.bussiness.santaseservice.repository;

import com.bussiness.santaseservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsername(String username);

    @Query("""
                SELECT COUNT(u) > 0 FROM User u
                WHERE u.username = :username
                AND NOT EXISTS (
                    SELECT g FROM Game g
                    WHERE (g.firstPlayer.user = u OR g.secondPlayer.user = u)
                    AND g.winner IS NULL
                )
            """)
    boolean existsAndIsAvailable(@Param("username") String username);
}

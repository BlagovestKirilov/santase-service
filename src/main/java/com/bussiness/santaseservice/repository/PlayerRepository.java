package com.bussiness.santaseservice.repository;

import com.bussiness.santaseservice.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
    Optional<Player> findByUserUsername(String username);
}
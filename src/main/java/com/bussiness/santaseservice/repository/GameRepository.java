package com.bussiness.santaseservice.repository;

import com.bussiness.santaseservice.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {
}

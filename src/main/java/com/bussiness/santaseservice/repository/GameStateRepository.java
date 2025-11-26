package com.bussiness.santaseservice.repository;

import com.bussiness.santaseservice.model.GameState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameStateRepository extends JpaRepository<GameState, Integer> {
}

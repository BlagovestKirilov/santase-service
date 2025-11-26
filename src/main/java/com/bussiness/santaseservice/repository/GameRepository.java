package com.bussiness.santaseservice.repository;

import com.bussiness.santaseservice.model.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}

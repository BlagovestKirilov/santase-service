package com.bussiness.santaseservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Game extends BaseEntity {

    @ManyToOne
    private User firstPlayer;

    @ManyToOne
    private User secondPlayer;

    @OneToOne
    private GameState state;

    private Integer firstPlayerResult;

    private Integer secondPlayerResult;
}

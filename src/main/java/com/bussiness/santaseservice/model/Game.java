package com.bussiness.santaseservice.model;

import jakarta.persistence.CascadeType;
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

    @OneToOne(cascade = CascadeType.ALL)
    private GameState state;

    @ManyToOne
    private User winner;

    private Integer firstPlayerResult;

    private Integer secondPlayerResult;
}

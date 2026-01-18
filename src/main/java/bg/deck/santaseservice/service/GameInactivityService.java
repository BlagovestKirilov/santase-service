package bg.deck.santaseservice.service;

import bg.deck.santaseservice.model.Game;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
public class GameInactivityService {

    private final ScheduledExecutorService scheduler;
    private final ExecutorService virtualThreadExecutor;
    private final GameUtilService gameUtilService;

    private final Map<UUID, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public void updateNextMoveTime(Game game) {
        if (game.getWinner() != null) {
            cancel(game.getId());
            return;
        }
        ScheduledFuture<?> old = tasks.remove(game.getId());

        if (old != null) {
            old.cancel(false);
        }

        ScheduledFuture<?> future = scheduler.schedule(() ->
                        virtualThreadExecutor.submit(() -> gameUtilService.surrenderByInactivity(game.getId()))
                , 33, TimeUnit.SECONDS);

        tasks.put(game.getId(), future);
    }

    public void cancel(UUID gameId) {
        ScheduledFuture<?> f = tasks.remove(gameId);
        if (f != null) {
            f.cancel(false);
        }
    }
}

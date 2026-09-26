package bg.deck.model.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Every game on offer, as read at one moment.
 *
 * <p>The map and the moment travel together so a reader can never see a fresh
 * list with a stale timestamp, or the other way about.
 *
 * @param byCode   the games, by their code
 * @param readAt   when the table was read
 */
public record ServiceCatalog(Map<String, ServiceAvailability> byCode, Instant readAt) {

    public ServiceCatalog {
        byCode = Map.copyOf(byCode);
    }

    public static ServiceCatalog empty() {
        return new ServiceCatalog(Map.of(), Instant.EPOCH);
    }

    public boolean isStale(Duration freshFor) {
        return readAt.plus(freshFor).isBefore(Instant.now());
    }
}

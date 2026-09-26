package bg.deck.service;

import bg.deck.enums.Scope;
import bg.deck.enums.ServiceState;
import bg.deck.model.AvailableService;
import bg.deck.model.User;
import bg.deck.model.dto.ServiceAvailability;
import bg.deck.model.dto.ServiceCatalog;
import bg.deck.repository.AvailableServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Which games are being offered, and to whom: the only place
 * {@link AvailableServiceRepository} is spoken to.
 *
 * <p>The catalogue is small and read on almost every request, so it is held for
 * half a minute at a time. That window is also the promise made to whoever
 * turns a game off: an UPDATE against the table takes effect within it, with no
 * deploy and no restart.
 *
 * <p>A game that is on and public is answered without reading the account at
 * all — which is every request, most days. The scope is fetched only when a
 * game asks for more than {@link Scope#PUBLIC}, and through
 * {@link UserAccountService}, never the user table directly.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class AvailabilityService {

    /** How long a read of the table is trusted for. */
    public static final Duration FRESH_FOR = Duration.ofSeconds(30);

    private final AvailableServiceRepository availableServiceRepository;
    private final UserAccountService userAccountService;

    private final AtomicReference<ServiceCatalog> catalog = new AtomicReference<>(ServiceCatalog.empty());

    /**
     * True when this player may start a game of this kind.
     *
     * <p>A code with no row is not offered. That is deliberate: a game is
     * visible once somebody has said so in the table, not merely because the
     * code exists somewhere in the source.
     */
    public boolean isAvailable(String code, String username) {
        ServiceAvailability service = catalog().byCode().get(code);
        if (service == null || service.state() != ServiceState.ON) {
            return false;
        }
        if (service.requiredScope() == Scope.PUBLIC) {
            return true;
        }
        return scopeOf(username).covers(service.requiredScope());
    }

    /** Everything this player may see, alphabetically. */
    public List<String> availableTo(String username) {
        Map<String, ServiceAvailability> services = catalog().byCode();

        boolean anyNeedsAScope = services.values().stream()
                .anyMatch(service -> service.state() == ServiceState.ON
                        && service.requiredScope() != Scope.PUBLIC);
        Scope scope = anyNeedsAScope ? scopeOf(username) : Scope.PUBLIC;

        return services.values().stream()
                .filter(service -> service.state() == ServiceState.ON)
                .filter(service -> scope.covers(service.requiredScope()))
                .map(ServiceAvailability::code)
                .sorted()
                .toList();
    }

    /** Drops the cache, so the next read goes to the table. */
    public void forget() {
        catalog.set(ServiceCatalog.empty());
    }

    private ServiceCatalog catalog() {
        ServiceCatalog held = catalog.get();
        if (!held.isStale(FRESH_FOR)) {
            return held;
        }
        ServiceCatalog fresh = read();
        catalog.set(fresh);
        return fresh;
    }

    @Transactional(readOnly = true)
    protected ServiceCatalog read() {
        Map<String, ServiceAvailability> byCode = availableServiceRepository.findAll().stream()
                .map(AvailabilityService::copyOf)
                .collect(Collectors.toMap(ServiceAvailability::code, Function.identity()));

        log.debug("Services on offer: {}", byCode.keySet());
        return new ServiceCatalog(byCode, Instant.now());
    }

    private static ServiceAvailability copyOf(AvailableService service) {
        return new ServiceAvailability(service.getCode(), service.getState(), service.getRequiredScope());
    }

    /** An account nobody can find is treated as ordinary, and sees the ordinary games. */
    private Scope scopeOf(String username) {
        return userAccountService.findByUsername(username)
                .map(User::getScope)
                .orElse(Scope.PUBLIC);
    }
}

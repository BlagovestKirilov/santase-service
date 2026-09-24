package bg.deck.belot.service;

import bg.deck.belot.model.BelotPlayer;
import bg.deck.belot.repository.BelotPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The players belot knows: the only place {@link BelotPlayerRepository} is
 * spoken to.
 *
 * <p>Nobody registers for belot. The row appears the first time someone
 * authenticated asks belot for anything, built from the name in their token —
 * the same shape as the provisioning filter the Keycloak work needs, and the
 * reason belot never has to read the user table.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class BelotPlayerService {

    private final BelotPlayerRepository belotPlayerRepository;

    /** The player under this name, created on first sight. */
    @Transactional
    public BelotPlayer ensureKnown(String username) {
        return belotPlayerRepository.findByUsername(username)
                .orElseGet(() -> {
                    log.info("Belot: first game for {}", username);
                    return belotPlayerRepository.save(new BelotPlayer(username));
                });
    }
}

package bg.deck.belot.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * The secret a table's shuffles come from, and the promise made about it.
 *
 * <p>A game starts with a random seed the server keeps and a hash of it the
 * table is shown. Every deal is shuffled from that seed and the deal's number,
 * so no shuffle can be chosen after the fact: the seed was committed to before
 * a card was dealt. At the end the seed is published and anyone can replay every
 * deal from it.
 *
 * <p>The same arrangement as табла's dice, for the same reason.
 */
@Log4j2
@Service
public class BelotSeedService {

    private static final String HMAC = "HmacSHA256";
    private static final int SEED_BYTES = 32;

    /**
     * Not {@code getInstanceStrong()}: on a small Linux box that blocks on the
     * entropy pool, and this is a card game, not a key ceremony.
     */
    private final SecureRandom random = new SecureRandom();

    public byte[] newSeed() {
        byte[] seed = new byte[SEED_BYTES];
        random.nextBytes(seed);
        return seed;
    }

    /** What the table is shown before the first deal. */
    public String hash(byte[] seed) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(seed));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is missing from this JVM", e);
        }
    }

    /**
     * The shuffle for one deal: the same seed and number always give the same
     * order of cards, and no other number gives it away.
     */
    public Random shuffleFor(byte[] seed, int dealNumber) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(seed, HMAC));
            byte[] digest = mac.doFinal(("deal-" + dealNumber).getBytes(StandardCharsets.UTF_8));

            long value = 0;
            for (int i = 0; i < Long.BYTES; i++) {
                value = (value << 8) | (digest[i] & 0xFFL);
            }
            return new Random(value);
        } catch (Exception e) {
            throw new IllegalStateException("Could not derive a shuffle", e);
        }
    }
}

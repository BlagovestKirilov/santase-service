package bg.deck.santaseservice.enums;

/**
 * Which game a {@link bg.deck.santaseservice.model.Game} row belongs to.
 *
 * <p>Every pre-existing row is {@code SANTASE}; the backfill in changeset 010
 * sets that explicitly before the column is made NOT NULL.
 */
public enum GameType {
    SANTASE,
    TABLA
}

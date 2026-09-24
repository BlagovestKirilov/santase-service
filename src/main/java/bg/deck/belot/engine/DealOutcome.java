package bg.deck.belot.engine;

/**
 * One deal, settled.
 *
 * @param result   how it went for the team that called it
 * @param recorded what each side writes on the sheet
 * @param hanging  points left waiting for whoever wins the next deal
 */
public record DealOutcome(DealResult result, RecordedScore recorded, int hanging) {
}

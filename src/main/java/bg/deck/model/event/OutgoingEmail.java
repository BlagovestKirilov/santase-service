package bg.deck.model.event;

/**
 * One account email, fully built, waiting to be sent.
 *
 * <p>Published by {@code EmailService} on the thread that asked for the email
 * and delivered by its after-commit listener on another one — so it carries
 * only plain strings, never an entity that would need a session to read.
 *
 * @param to      the recipient's address
 * @param subject the subject line
 * @param html    the rendered body
 */
public record OutgoingEmail(String to, String subject, String html) {
}

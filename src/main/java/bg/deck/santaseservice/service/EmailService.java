package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TemplateLoader;
import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.UserDeletion;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static bg.deck.santaseservice.constant.Constants.DECK_BG_CONFIRM_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_DELETE_ACCOUNT;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_FORGOT_PASSWORD;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_PERSONAL;
import static bg.deck.santaseservice.constant.Constants.DELETION_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.DELETION_TEMPLATE;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_LINK;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_TEMPLATE;
import static bg.deck.santaseservice.constant.Constants.EMAIL_USERNAME;
import static bg.deck.santaseservice.constant.Constants.FORGOT_PASSWORD_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.FORGOT_PASSWORD_TEMPLATE;

/**
 * Sends the account emails — confirmation, new password, deletion — without
 * making the request wait for the mail server.
 *
 * <p>Each email is built completely on the thread that asked for it: the
 * address comes from a lazily loaded user, which cannot be read once the
 * request has finished. Only plain strings cross to the sending thread.
 *
 * <p>It is sent after the caller's transaction commits, so a link is never
 * mailed for a token that was then rolled back; a caller with no transaction
 * has nothing to wait for, and the email goes at once. A failure is logged —
 * there is no request left to fail — and the person can ask again.
 */
@Log4j2
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateLoader templateLoader;
    private final ApplicationEventPublisher events;

    /** One email, fully built, waiting to be sent. */
    public record OutgoingEmail(String to, String subject, String html) {
    }

    public void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        queue(emailConfirmation.getUser().getEmail(), DECK_BG_EMAIL_SUBJECT, buildConfirmationBody(emailConfirmation));
    }

    public void sendForgotPasswordEmail(ForgotPassword forgotPassword) {
        queue(forgotPassword.getUser().getEmail(), FORGOT_PASSWORD_SUBJECT, buildForgotPasswordBody(forgotPassword));
    }

    public void sendDeletionEmail(UserDeletion userDeletion) {
        queue(userDeletion.getUser().getEmail(), DELETION_SUBJECT, buildDeletionBody(userDeletion));
    }

    private void queue(String to, String subject, String html) {
        events.publishEvent(new OutgoingEmail(to, subject, html));
    }

    /**
     * Runs on its own (virtual) thread once the caller's transaction has
     * committed, or straight away when there was none — without
     * {@code fallbackExecution}, an email queued outside a transaction would
     * never be sent at all.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void deliver(OutgoingEmail email) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());

            helper.setFrom(sender());
            helper.setTo(email.to());
            helper.setSubject(email.subject());
            helper.setText(email.html(), true);

            javaMailSender.send(message);
            log.info(LogConstants.EMAIL_SENT_LOG, email.to());
        } catch (MessagingException | MailException ex) {
            log.error(LogConstants.EMAIL_SEND_FAILED, email.to(), ex);
        }
    }

    /**
     * The sender, shown as DECK.bg with no quotes around it.
     *
     * <p>A dot is a special character in an address's display name, so written
     * plainly the name has to go out as a quoted string — {@code "DECK.bg" <…>}
     * — and some webmail shows those quotes as part of the name. An RFC 2047
     * encoded-word carries the same name unquoted, and every client decodes
     * one: it is how any Cyrillic sender name travels.
     */
    static InternetAddress sender() throws AddressException {
        String name = Base64.getEncoder().encodeToString(DECK_BG_PERSONAL.getBytes(StandardCharsets.UTF_8));
        return new InternetAddress("=?UTF-8?B?" + name + "?= <" + DECK_BG_EMAIL + ">");
    }

    private String buildConfirmationBody(EmailConfirmation emailConfirmation) {
        String link = DECK_BG_CONFIRM_EMAIL + emailConfirmation.getConfirmationToken();
        return buildEmailBody(EMAIL_CONFIRMATION_TEMPLATE, emailConfirmation.getUser().getUsername(), link);
    }

    private String buildForgotPasswordBody(ForgotPassword forgotPassword) {
        String link = DECK_BG_FORGOT_PASSWORD + forgotPassword.getForgotPasswordToken();
        return buildEmailBody(FORGOT_PASSWORD_TEMPLATE, forgotPassword.getUser().getUsername(), link);
    }

    private String buildDeletionBody(UserDeletion userDeletion) {
        String link = DECK_BG_DELETE_ACCOUNT + userDeletion.getUserDeletionToken();
        return buildEmailBody(DELETION_TEMPLATE, userDeletion.getUser().getUsername(), link);
    }

    private String buildEmailBody(String templatePath, String username, String link) {
        String template = templateLoader.load(templatePath);
        return template
                .replace(EMAIL_USERNAME, username)
                .replace(EMAIL_CONFIRMATION_LINK, link);
    }
}

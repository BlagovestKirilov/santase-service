package bg.deck;

import bg.deck.enums.EmailConfirmationStatus;
import bg.deck.enums.ForgotPasswordStatus;
import bg.deck.enums.UserDeletionStatus;
import bg.deck.repository.EmailConfirmationRepository;
import bg.deck.repository.ForgotPasswordRepository;
import bg.deck.repository.UserDeletionRepository;
import bg.deck.scheduler.ExpiredLinkScheduler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.time.Instant;

import static bg.deck.constant.Constants.EMAIL_CONFIRMATION_VALIDITY;
import static bg.deck.constant.Constants.LINK_VALIDITY;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The job that keeps the tables saying what is true: a link nobody opened is
 * marked expired once its time is up, instead of sitting at PENDING for good.
 */
@DisplayName("Retiring the links nobody came back for")
@ExtendWith(MockitoExtension.class)
class ExpiredLinkSchedulerTest {

    @Mock private ForgotPasswordRepository forgotPasswordRepository;
    @Mock private EmailConfirmationRepository emailConfirmationRepository;
    @Mock private UserDeletionRepository userDeletionRepository;
    @InjectMocks private ExpiredLinkScheduler scheduler;

    @Test
    @DisplayName("each table is retired to its own cutoff, PENDING to EXPIRED")
    void retiresEachTableOnItsOwnCutoff() {
        Instant before = Instant.now();

        scheduler.expireLinks();

        Instant after = Instant.now();

        // A reset and a deletion go after a quarter of an hour.
        assertCutoff(captureCutoff(ForgotPasswordRepository.class), LINK_VALIDITY, before, after);
        assertCutoff(captureCutoff(UserDeletionRepository.class), LINK_VALIDITY, before, after);
        // A new player's confirmation has the whole day.
        assertCutoff(captureCutoff(EmailConfirmationRepository.class), EMAIL_CONFIRMATION_VALIDITY, before, after);
    }


    /**
     * What the line in the log actually says. Five numbers in one message is
     * where an argument slips a place and nobody notices, because the sentence
     * still reads perfectly well.
     */
    @Test
    @DisplayName("the line says how many of each were retired")
    void logsWhatItRetired() {
        when(forgotPasswordRepository.expireOlderThan(any(), any(), any())).thenReturn(3);
        when(emailConfirmationRepository.expireOlderThan(any(), any(), any())).thenReturn(7);
        when(userDeletionRepository.expireOlderThan(any(), any(), any())).thenReturn(1);

        Capture capture = captureLogs(Level.INFO);
        scheduler.expireLinks();

        assertEquals(1, capture.lines.size(), "one line for the run");
        String line = capture.lines.getFirst();
        assertTrue(line.startsWith("Retired 11 expired links: 3 password resets, "
                + "7 email confirmations, 1 account deletions."), "but it said: " + line);
    }

    @Test
    @DisplayName("a run that retired nothing keeps out of the log")
    void quietWhenThereIsNothingToRetire() {
        Capture capture = captureLogs(Level.INFO);

        scheduler.expireLinks();

        assertEquals(List.of(), capture.lines, "288 runs a day, and nothing to say about most of them");
    }

    /* ---------------- helpers ---------------- */

    private Instant captureCutoff(Class<?> repository) {
        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        if (repository == ForgotPasswordRepository.class) {
            verify(forgotPasswordRepository).expireOlderThan(
                    cutoff.capture(),
                    eq(ForgotPasswordStatus.PENDING),
                    eq(ForgotPasswordStatus.EXPIRED));
        } else if (repository == EmailConfirmationRepository.class) {
            verify(emailConfirmationRepository).expireOlderThan(
                    cutoff.capture(),
                    eq(EmailConfirmationStatus.PENDING),
                    eq(EmailConfirmationStatus.EXPIRED));
        } else {
            verify(userDeletionRepository).expireOlderThan(
                    cutoff.capture(),
                    eq(UserDeletionStatus.PENDING),
                    eq(UserDeletionStatus.EXPIRED));
        }
        return cutoff.getValue();
    }

    /** The cutoff is exactly one lifetime back from the moment of the run. */
    private static void assertCutoff(Instant cutoff, Duration validity, Instant before, Instant after) {
        assertTrue(!cutoff.isBefore(before.minus(validity)) && !cutoff.isAfter(after.minus(validity)),
                "taken back by " + validity + ", but the cutoff was " + cutoff);
    }

    @Test
    @DisplayName("a run that finds nothing still visits all three")
    void visitsAllThreeEvenWhenEmpty() {
        scheduler.expireLinks();

        verify(forgotPasswordRepository).expireOlderThan(any(), any(), any());
        verify(emailConfirmationRepository).expireOlderThan(any(), any(), any());
        verify(userDeletionRepository).expireOlderThan(any(), any(), any());
    }

    /** Log4j2 without a whole configuration: one appender that keeps the lines. */
    private static final class Capture extends AbstractAppender {
        private final List<String> lines = new CopyOnWriteArrayList<>();

        private Capture() {
            super("capture", null, null, true, Property.EMPTY_ARRAY);
        }

        @Override
        public void append(LogEvent event) {
            lines.add(event.getMessage().getFormattedMessage());
        }
    }

    private Logger scheduledLogger;
    private Capture appender;

    private Capture captureLogs(Level level) {
        scheduledLogger = (Logger) org.apache.logging.log4j.LogManager.getLogger(ExpiredLinkScheduler.class);
        appender = new Capture();
        appender.start();
        scheduledLogger.addAppender(appender);
        scheduledLogger.setLevel(level);
        return appender;
    }

    @AfterEach
    void detachAppender() {
        if (scheduledLogger != null) {
            scheduledLogger.removeAppender(appender);
            scheduledLogger = null;
        }
    }
}

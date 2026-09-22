package bg.deck.santaseservice.service;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The sender's name reaches the inbox as DECK.bg — not "DECK.bg" with the
 * quotes a plain display name containing a dot has to carry.
 */
@DisplayName("The email sender")
class EmailSenderTest {

    @Test
    @DisplayName("goes out with no quotes in the From header")
    void noQuotesInTheHeader() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(EmailService.sender());
        message.saveChanges();

        String header = message.getHeader("From")[0];
        assertFalse(header.contains("\""), "no quoted string in: " + header);
        assertEquals("=?UTF-8?B?REVDSy5iZw==?= <no.reply.deck.bg@gmail.com>", header);
    }

    @Test
    @DisplayName("reads back as DECK.bg, which is what a mail client shows")
    void decodesToTheBrand() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        message.setFrom(EmailService.sender());
        message.saveChanges();

        InternetAddress from = (InternetAddress) message.getFrom()[0];
        assertEquals("DECK.bg", from.getPersonal());
        assertEquals("no.reply.deck.bg@gmail.com", from.getAddress());
    }
}

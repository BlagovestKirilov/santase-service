package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TemplateLoader;
import bg.deck.santaseservice.exception.NotSendEmailException;
import bg.deck.santaseservice.model.EmailConfirmation;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

import static bg.deck.santaseservice.constant.Constants.DECK_BG_CONFIRM_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL_ENCODING;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_PERSONAL;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_LINK;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_TEMPLATE;
import static bg.deck.santaseservice.constant.Constants.EMAIL_USERNAME;
import static bg.deck.santaseservice.constant.LogConstants.EMAIL_SENT_LOG;

@Log4j2
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateLoader templateLoader;

    public void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, DECK_BG_EMAIL_ENCODING);

            helper.setFrom(DECK_BG_EMAIL, DECK_BG_PERSONAL);
            helper.setTo(emailConfirmation.getUser().getEmail());
            helper.setSubject(DECK_BG_EMAIL_SUBJECT);
            helper.setText(buildConfirmationBody(emailConfirmation), true);

            javaMailSender.send(message);
            log.info(EMAIL_SENT_LOG, emailConfirmation.getUser().getEmail());
        } catch (MessagingException | UnsupportedEncodingException _) {
            throw new NotSendEmailException(emailConfirmation.getUser().getEmail());
        }
    }

    private String buildConfirmationBody(EmailConfirmation emailConfirmation) {
        String link = DECK_BG_CONFIRM_EMAIL + emailConfirmation.getConfirmationToken();

        String template = templateLoader.load(EMAIL_CONFIRMATION_TEMPLATE);

        return template
                .replace(EMAIL_USERNAME, emailConfirmation.getUser().getUsername())
                .replace(EMAIL_CONFIRMATION_LINK, link);
    }
}

package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TemplateLoader;
import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.exception.NotSendEmailException;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.ForgotPassword;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static bg.deck.santaseservice.constant.Constants.DECK_BG_CONFIRM_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_EMAIL_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_FORGOT_PASSWORD;
import static bg.deck.santaseservice.constant.Constants.DECK_BG_PERSONAL;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_LINK;
import static bg.deck.santaseservice.constant.Constants.EMAIL_CONFIRMATION_TEMPLATE;
import static bg.deck.santaseservice.constant.Constants.EMAIL_USERNAME;
import static bg.deck.santaseservice.constant.Constants.FORGOT_PASSWORD_SUBJECT;
import static bg.deck.santaseservice.constant.Constants.FORGOT_PASSWORD_TEMPLATE;

@Log4j2
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateLoader templateLoader;

    public void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        String email = emailConfirmation.getUser().getEmail();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());

            helper.setFrom(DECK_BG_EMAIL, DECK_BG_PERSONAL);
            helper.setTo(email);
            helper.setSubject(DECK_BG_EMAIL_SUBJECT);
            helper.setText(buildConfirmationBody(emailConfirmation), true);

            javaMailSender.send(message);

            log.info(LogConstants.EMAIL_SENT_LOG, email);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error(LogConstants.EMAIL_SEND_FAILED, email, ex);
            throw new NotSendEmailException(email);
        }
    }

    public void sendForgotPasswordEmail(ForgotPassword forgotPassword) {
        String email = forgotPassword.getUser().getEmail();

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());

            helper.setFrom(DECK_BG_EMAIL, DECK_BG_PERSONAL);
            helper.setTo(email);
            helper.setSubject(FORGOT_PASSWORD_SUBJECT);
            helper.setText(buildForgotPasswordBody(forgotPassword), true);

            javaMailSender.send(message);

            log.info(LogConstants.EMAIL_SENT_LOG, email);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error(LogConstants.EMAIL_SEND_FAILED, email, ex);
            throw new NotSendEmailException(email);
        }
    }

    private String buildConfirmationBody(EmailConfirmation emailConfirmation) {
        String link = DECK_BG_CONFIRM_EMAIL + emailConfirmation.getConfirmationToken();

        String template = templateLoader.load(EMAIL_CONFIRMATION_TEMPLATE);

        return template
                .replace(EMAIL_USERNAME, emailConfirmation.getUser().getUsername())
                .replace(EMAIL_CONFIRMATION_LINK, link);
    }

    private String buildForgotPasswordBody(ForgotPassword forgotPassword) {
        String link = DECK_BG_FORGOT_PASSWORD + forgotPassword.getForgotPasswordToken();

        String template = templateLoader.load(FORGOT_PASSWORD_TEMPLATE);

        return template
                .replace(EMAIL_USERNAME, forgotPassword.getUser().getUsername())
                .replace(EMAIL_CONFIRMATION_LINK, link);
    }
}

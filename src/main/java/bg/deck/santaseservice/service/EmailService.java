package bg.deck.santaseservice.service;

import bg.deck.santaseservice.config.TemplateLoader;
import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.exception.NotSendEmailException;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.UserDeletion;
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

@Log4j2
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender javaMailSender;
    private final TemplateLoader templateLoader;

    public void sendConfirmationEmail(EmailConfirmation emailConfirmation) {
        String email = emailConfirmation.getUser().getEmail();
        String body = buildConfirmationBody(emailConfirmation);
        sendEmail(email, DECK_BG_EMAIL_SUBJECT, body);
    }

    public void sendForgotPasswordEmail(ForgotPassword forgotPassword) {
        String email = forgotPassword.getUser().getEmail();
        String body = buildForgotPasswordBody(forgotPassword);
        sendEmail(email, FORGOT_PASSWORD_SUBJECT, body);
    }

    public void sendDeletionEmail(UserDeletion userDeletion) {
        String email = userDeletion.getUser().getEmail();
        String body = buildDeletionBody(userDeletion);
        sendEmail(email, DELETION_SUBJECT, body);
    }

    private void sendEmail(String toEmail, String subject, String htmlBody) {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.toString());

            helper.setFrom(DECK_BG_EMAIL, DECK_BG_PERSONAL);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            javaMailSender.send(message);
            log.info(LogConstants.EMAIL_SENT_LOG, toEmail);

        } catch (MessagingException | UnsupportedEncodingException ex) {
            log.error(LogConstants.EMAIL_SEND_FAILED, toEmail, ex);
            throw new NotSendEmailException(toEmail);
        }
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

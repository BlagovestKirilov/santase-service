package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.enums.EmailConfirmationStatus;
import bg.deck.santaseservice.exception.EmailConfirmationNotFoundException;
import bg.deck.santaseservice.exception.InvalidCredentialsException;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.response.ProfileResponse;
import bg.deck.santaseservice.repository.EmailConfirmationRepository;
import bg.deck.santaseservice.repository.UserRepository;
import bg.deck.santaseservice.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Log4j2
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final EmailConfirmationRepository emailConfirmationRepository;
    private final GameUtilService gameUtilService;
    private final EmailService emailService;
    private final UserMapper userMapper;

    public ProfileResponse getProfile() {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.TRY_GET_PROFILE, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        return userMapper.toProfileResponse(user);
    }

    public boolean resendEmail() {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.EMAIL_RESEND_ATTEMPT, username);

        EmailConfirmation emailConfirmation =
                emailConfirmationRepository.findByUserUsername(username)
                        .orElseThrow(() -> {
                            log.warn(LogConstants.EMAIL_RESEND_CONFIRMATION_NOT_FOUND, username);
                            return new EmailConfirmationNotFoundException(username);
                        });

        if (Boolean.TRUE.equals(emailConfirmation.getUser().getIsEmailConfirmed())
                || EmailConfirmationStatus.CONFIRMED.equals(emailConfirmation.getStatus())) {

            log.info(LogConstants.EMAIL_RESEND_ALREADY_CONFIRMED, username);
            return false;
        }

        emailConfirmation.setConfirmationToken(UUID.randomUUID().toString());
        emailConfirmationRepository.save(emailConfirmation);

        emailService.sendConfirmationEmail(emailConfirmation);

        log.info(LogConstants.EMAIL_RESEND_SUCCESS, username);

        return true;
    }

}

package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.ExceptionConstants;
import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.enums.EmailConfirmationStatus;
import bg.deck.santaseservice.enums.UserDeletionStatus;
import bg.deck.santaseservice.exception.EmailNotConfirmedException;
import bg.deck.santaseservice.exception.InvalidCredentialsException;
import bg.deck.santaseservice.exception.InvalidPasswordException;
import bg.deck.santaseservice.exception.UserNotFoundException;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.UserDeletion;
import bg.deck.santaseservice.model.request.ChangePasswordRequest;
import bg.deck.santaseservice.model.request.UserDeletionRequest;
import bg.deck.santaseservice.constant.RankingConstants;
import bg.deck.santaseservice.enums.GameType;
import bg.deck.santaseservice.model.UserGameStats;
import bg.deck.santaseservice.model.response.GameStatsDTO;
import bg.deck.santaseservice.model.response.ProfileResponse;
import bg.deck.santaseservice.repository.EmailConfirmationRepository;
import bg.deck.santaseservice.repository.UserDeletionRepository;
import bg.deck.santaseservice.repository.UserRepository;
import bg.deck.santaseservice.util.UserMapper;
import lombok.RequiredArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
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
    private final PasswordEncoder passwordEncoder;
    private final UserUtilService userUtilService;
    private final UserDeletionRepository userDeletionRepository;

    public ProfileResponse getProfile() {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.TRY_GET_PROFILE, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        ProfileResponse response = new ProfileResponse();
        response.setEmailConfirmed(Boolean.TRUE.equals(user.getIsEmailConfirmed()));

        Map<String, GameStatsDTO> stats = new LinkedHashMap<>();
        for (GameType type : GameType.values()) {
            UserGameStats s = user.statsFor(type);
            int remaining = Math.max(0, RankingConstants.PLACEMENT_GAMES - s.totalGames());
            stats.put(type.name(), new GameStatsDTO(
                    s.getWins(), s.getLosses(), s.getRating(), s.getRank().name(), remaining));
        }
        response.setStats(stats);

        // Legacy shape for the client that is deployed right now.
        UserGameStats santase = user.statsFor(GameType.SANTASE);
        response.setSantaseWins(santase.getWins());
        response.setSantaseLosses(santase.getLosses());
        response.setRank(santase.getRank().name());

        return response;
    }

    public boolean confirmEmail() {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.EMAIL_CONFIRM_ATTEMPT, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn(LogConstants.USER_NOT_FOUND, username);
                    return new UserNotFoundException(username);
                });

        if (Boolean.TRUE.equals(user.getIsEmailConfirmed())) {
            log.info(LogConstants.EMAIL_CONFIRMATION_ALREADY_CONFIRMED, username);
            return false;
        }

        List<EmailConfirmation> pendingEmailConfirmations = emailConfirmationRepository
                .findAllByUserAndStatus(user, EmailConfirmationStatus.PENDING);

        pendingEmailConfirmations.forEach(ec -> ec.setStatus(EmailConfirmationStatus.EXPIRED));
        emailConfirmationRepository.saveAll(pendingEmailConfirmations);

        EmailConfirmation emailConfirmation = new EmailConfirmation(user);
        emailConfirmationRepository.save(emailConfirmation);

        emailService.sendConfirmationEmail(emailConfirmation);

        log.info(LogConstants.EMAIL_SEND_SUCCESS, username);

        return true;
    }

    @Transactional
    public void changePassword(ChangePasswordRequest changePasswordRequest) {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.PASSWORD_CHANGE_STARTED, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn(LogConstants.USER_NOT_FOUND, username);
                    return new UserNotFoundException(username);
                });

        if (Boolean.FALSE.equals(user.getIsEmailConfirmed())) {
            log.warn(LogConstants.EMAIL_NOT_CONFIRMED, username);
            throw new EmailNotConfirmedException(user.getEmail());
        }

        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), user.getPassword())) {
            log.warn(LogConstants.INVALID_CURRENT_PASSWORD, username);
            throw new InvalidCredentialsException(username);
        }

        if (passwordEncoder.matches(changePasswordRequest.getNewPassword(), user.getPassword())) {
            log.warn(LogConstants.SAME_PASSWORD, username);
            throw new InvalidPasswordException(ExceptionConstants.SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        userRepository.save(user);

        log.info(LogConstants.PASSWORD_CHANGE_SUCCESS, username);
    }

    @Transactional
    public void sendUserDeletionEmail(UserDeletionRequest userDeletionRequest) {
        String username = gameUtilService.getUsername();

        log.info(LogConstants.USER_DELETION_EMAIL_REQUESTED, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn(LogConstants.USER_NOT_FOUND, username);
                    return new UserNotFoundException(username);
                });

        if (Boolean.FALSE.equals(user.getIsEmailConfirmed())) {
            log.warn(LogConstants.EMAIL_NOT_CONFIRMED, username);
            throw new EmailNotConfirmedException(user.getEmail());
        }

        if (!passwordEncoder.matches(userDeletionRequest.getPassword(), user.getPassword())) {
            log.warn(LogConstants.INVALID_PASSWORD, username);
            throw new InvalidCredentialsException(username);
        }

        List<UserDeletion> pendingForgotPasswordList = userDeletionRepository
                .findAllByUserAndStatus(user, UserDeletionStatus.PENDING);

        pendingForgotPasswordList.forEach(pendingForgotPassword ->
                pendingForgotPassword.setStatus(UserDeletionStatus.EXPIRED));
        userDeletionRepository.saveAll(pendingForgotPasswordList);

        UserDeletion userDeletion = new UserDeletion(user);
        userDeletionRepository.save(userDeletion);
        log.info(LogConstants.USER_DELETION_RECORD_CREATED, username, userDeletion.getId());

        emailService.sendDeletionEmail(userDeletion);
        log.info(LogConstants.USER_DELETION_EMAIL_SENT, user.getEmail());
    }

    @Transactional
    public boolean confirmDeletion(UUID userDeletionToken) {
        log.info(LogConstants.USER_DELETION_CONFIRM_ATTEMPT, userDeletionToken);

        Optional<UserDeletion> optionalUserDeletion = userDeletionRepository
                .findByUserDeletionTokenAndStatus(userDeletionToken, UserDeletionStatus.PENDING);

        if (optionalUserDeletion.isEmpty()) {
            log.warn(LogConstants.USER_DELETION_TOKEN_INVALID);
            return false;
        }

        UserDeletion userDeletion = optionalUserDeletion.get();
        String username = userDeletion.getUser().getUsername();

        log.info(LogConstants.USER_DELETION_CONFIRMED, username);
        log.info(LogConstants.USER_DELETION_STARTED, username);

        userUtilService.deleteUser(userDeletion.getUser());

        userDeletion.setStatus(UserDeletionStatus.SUCCESS);
        userDeletionRepository.save(userDeletion);

        log.info(LogConstants.USER_DELETION_SUCCESS, username);
        return true;
    }
}

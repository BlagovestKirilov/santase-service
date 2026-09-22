package bg.deck.service;

import bg.deck.constant.ExceptionConstants;
import bg.deck.constant.LogConstants;
import bg.deck.enums.EmailConfirmationStatus;
import bg.deck.enums.ForgotPasswordStatus;
import bg.deck.exception.EmailAlreadyExistsException;
import bg.deck.exception.EmailNotConfirmedException;
import bg.deck.exception.InvalidCredentialsException;
import bg.deck.exception.InvalidLinkException;
import bg.deck.exception.InvalidPasswordException;
import bg.deck.exception.InvalidTokenException;
import bg.deck.exception.UserAlreadyExistsException;
import bg.deck.model.EmailConfirmation;
import bg.deck.model.ForgotPassword;
import bg.deck.enums.GameType;
import bg.deck.model.User;
import bg.deck.model.UserGameStats;
import bg.deck.model.request.ChangeForgottenPasswordRequest;
import bg.deck.model.request.ForgotPasswordEmailRequest;
import bg.deck.model.request.LoginRequest;
import bg.deck.model.request.RegisterRequest;
import bg.deck.model.response.AuthResponse;
import bg.deck.repository.EmailConfirmationRepository;
import bg.deck.repository.ForgotPasswordRepository;
import bg.deck.repository.PlayerRepository;
import bg.deck.repository.UserRepository;
import bg.deck.util.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static bg.deck.constant.Constants.CF_CONNECTING_IP;
import static bg.deck.constant.LogConstants.SUCCESSFUL_LOGIN_LOG;
import static bg.deck.constant.LogConstants.SUCCESSFUL_REGISTER_LOG;
import static bg.deck.constant.LogConstants.TRY_LOGIN_LOG;
import static bg.deck.constant.LogConstants.TRY_REGISTER_LOG;

@Log4j2
@Transactional
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final EmailConfirmationRepository emailConfirmationRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        log.info(TRY_LOGIN_LOG, loginRequest.username());

        User user = userRepository.findByUsername(loginRequest.username())
                .filter(foundUser -> passwordEncoder.matches(loginRequest.password(), foundUser.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(loginRequest.username()));

        log.info(SUCCESSFUL_LOGIN_LOG, loginRequest.username());

        user.setIpAddress(request.getHeader(CF_CONNECTING_IP));
        userRepository.save(user);

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(LogConstants.SUCCESSFUL_LOGIN)
                .token(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        log.info(TRY_REGISTER_LOG, registerRequest.username());

        if (userRepository.existsByUsername(registerRequest.username())) {
            throw new UserAlreadyExistsException(registerRequest.username());
        }

        if (userRepository.existsByEmail(registerRequest.email())) {
            throw new EmailAlreadyExistsException(registerRequest.email());
        }

        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        // Both stats rows are created up front: lazy creation on first game would
        // need INSERT ... ON CONFLICT handling under concurrency, and one extra
        // row per user is cheaper than that.
        user.addStats(UserGameStats.fresh(user, GameType.SANTASE));
        user.addStats(UserGameStats.fresh(user, GameType.TABLA));
        userRepository.save(user);

        EmailConfirmation emailConfirmation = new EmailConfirmation(user);
        emailConfirmationRepository.save(emailConfirmation);

        emailService.sendConfirmationEmail(emailConfirmation);

        log.info(SUCCESSFUL_REGISTER_LOG, registerRequest.username());

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(LogConstants.SUCCESSFUL_REGISTER)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        log.info(LogConstants.TRY_REFRESH_TOKEN, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        if (username != null && jwtService.isTokenValid(refreshToken)) {

            log.info(LogConstants.SUCCESSFUL_REFRESH_TOKEN);

            return AuthResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .message(LogConstants.SUCCESSFUL_REFRESH_TOKEN)
                    .token(jwtService.generateToken(user))
                    .refreshToken(jwtService.generateRefreshToken(user))
                    .build();
        }

        throw new InvalidTokenException();
    }

    public boolean confirmEmail(UUID confirmationToken) {
        log.info(LogConstants.EMAIL_CONFIRMATION_ATTEMPT, confirmationToken);

        Optional<EmailConfirmation> optionalEmailConfirmation =
                emailConfirmationRepository.findByConfirmationTokenAndStatus(confirmationToken, EmailConfirmationStatus.PENDING);

        if (optionalEmailConfirmation.isEmpty()) {
            log.warn(LogConstants.EMAIL_CONFIRMATION_TOKEN_NOT_FOUND, confirmationToken);
            return false;
        }

        EmailConfirmation emailConfirmation = optionalEmailConfirmation.get();

        if (emailConfirmation.getUser() == null) {
            log.warn(LogConstants.EMAIL_CONFIRMATION_TOKEN_NOT_FOUND, confirmationToken);
            return false;
        }

        if (EmailConfirmationStatus.CONFIRMED.equals(emailConfirmation.getStatus())) {
            log.info(
                    LogConstants.EMAIL_ALREADY_CONFIRMED,
                    confirmationToken,
                    emailConfirmation.getUser().getUsername()
            );
            return false;
        }

        emailConfirmation.setStatus(EmailConfirmationStatus.CONFIRMED);
        emailConfirmation.getUser().setIsEmailConfirmed(true);

        emailConfirmationRepository.save(emailConfirmation);

        log.info(
                LogConstants.EMAIL_CONFIRMED_SUCCESSFULLY,
                confirmationToken,
                emailConfirmation.getUser().getUsername()
        );

        return true;
    }

    public void forgotPassword(ForgotPasswordEmailRequest forgotPasswordEmailRequest) {
        String email = forgotPasswordEmailRequest.email();

        log.info(LogConstants.FORGOT_PASSWORD_STARTED, email);

        User user = userRepository.findByEmail(email)
                .filter(u -> Boolean.TRUE.equals(u.getIsEmailConfirmed()))
                .orElseThrow(() -> {
                    log.warn(LogConstants.FORGOT_PASSWORD_EMAIL_NOT_CONFIRMED, email);
                    return new EmailNotConfirmedException(email);
                });

        List<ForgotPassword> pendingForgotPasswordList = forgotPasswordRepository
                .findAllByUserAndStatus(user, ForgotPasswordStatus.PENDING);

        pendingForgotPasswordList.forEach(pendingForgotPassword -> pendingForgotPassword.setStatus(ForgotPasswordStatus.EXPIRED));
        forgotPasswordRepository.saveAll(pendingForgotPasswordList);

        ForgotPassword forgotPassword = new ForgotPassword(user);
        forgotPasswordRepository.save(forgotPassword);

        emailService.sendForgotPasswordEmail(forgotPassword);

        log.info(LogConstants.FORGOT_PASSWORD_EMAIL_SENT, email);
    }

    public void changeForgottenPassword(ChangeForgottenPasswordRequest changeForgottenPasswordRequest) {
        ForgotPassword forgotPassword = forgotPasswordRepository
                .findByForgotPasswordTokenAndStatus(changeForgottenPasswordRequest.token(), ForgotPasswordStatus.PENDING)
                .orElseThrow(InvalidLinkException::new);

        User user = forgotPassword.getUser();

        log.info(LogConstants.PASSWORD_CHANGE_STARTED, user.getUsername());

        if (Boolean.FALSE.equals(user.getIsEmailConfirmed())) {
            log.warn(LogConstants.EMAIL_NOT_CONFIRMED, user.getUsername());
            throw new EmailNotConfirmedException(user.getEmail());
        }

        if (passwordEncoder.matches(changeForgottenPasswordRequest.newPassword(), user.getPassword())) {
            log.warn(LogConstants.SAME_PASSWORD, user.getUsername());
            throw new InvalidPasswordException(ExceptionConstants.SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(changeForgottenPasswordRequest.newPassword()));
        userRepository.save(user);

        forgotPassword.setStatus(ForgotPasswordStatus.SUCCESS);
        forgotPasswordRepository.save(forgotPassword);

        log.info(LogConstants.PASSWORD_CHANGE_SUCCESS, user.getUsername());
    }

    public void verifyForgotPasswordToken(UUID token) {
        forgotPasswordRepository
                .findByForgotPasswordTokenAndStatus(token, ForgotPasswordStatus.PENDING)
                .orElseThrow(InvalidLinkException::new);
    }
}

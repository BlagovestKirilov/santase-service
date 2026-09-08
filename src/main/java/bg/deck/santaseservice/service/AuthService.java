package bg.deck.santaseservice.service;

import bg.deck.santaseservice.constant.ExceptionConstants;
import bg.deck.santaseservice.constant.LogConstants;
import bg.deck.santaseservice.enums.EmailConfirmationStatus;
import bg.deck.santaseservice.enums.ForgotPasswordStatus;
import bg.deck.santaseservice.exception.EmailAlreadyExistsException;
import bg.deck.santaseservice.exception.EmailNotConfirmedException;
import bg.deck.santaseservice.exception.InvalidCredentialsException;
import bg.deck.santaseservice.exception.InvalidPasswordException;
import bg.deck.santaseservice.exception.InvalidTokenException;
import bg.deck.santaseservice.exception.UserAlreadyExistsException;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.enums.GameType;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.UserGameStats;
import bg.deck.santaseservice.model.request.ChangeForgottenPasswordRequest;
import bg.deck.santaseservice.model.request.ForgotPasswordEmailRequest;
import bg.deck.santaseservice.model.request.LoginRequest;
import bg.deck.santaseservice.model.request.RegisterRequest;
import bg.deck.santaseservice.model.response.AuthResponse;
import bg.deck.santaseservice.repository.EmailConfirmationRepository;
import bg.deck.santaseservice.repository.ForgotPasswordRepository;
import bg.deck.santaseservice.repository.PlayerRepository;
import bg.deck.santaseservice.repository.UserRepository;
import bg.deck.santaseservice.util.UserMapper;
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

import static bg.deck.santaseservice.constant.Constants.CF_CONNECTING_IP;
import static bg.deck.santaseservice.constant.LogConstants.SUCCESSFUL_LOGIN_LOG;
import static bg.deck.santaseservice.constant.LogConstants.SUCCESSFUL_REGISTER_LOG;
import static bg.deck.santaseservice.constant.LogConstants.TRY_LOGIN_LOG;
import static bg.deck.santaseservice.constant.LogConstants.TRY_REGISTER_LOG;

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
        log.info(TRY_LOGIN_LOG, loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .filter(foundUser -> passwordEncoder.matches(loginRequest.getPassword(), foundUser.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(loginRequest.getUsername()));

        log.info(SUCCESSFUL_LOGIN_LOG, loginRequest.getUsername());

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
        log.info(TRY_REGISTER_LOG, registerRequest.getUsername());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new UserAlreadyExistsException(registerRequest.getUsername());
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new EmailAlreadyExistsException(registerRequest.getEmail());
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

        log.info(SUCCESSFUL_REGISTER_LOG, registerRequest.getUsername());

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
        String email = forgotPasswordEmailRequest.getEmail();

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
                .findByForgotPasswordTokenAndStatus(changeForgottenPasswordRequest.getToken(), ForgotPasswordStatus.PENDING)
                .orElseThrow(InvalidTokenException::new);

        User user = forgotPassword.getUser();

        log.info(LogConstants.PASSWORD_CHANGE_STARTED, user.getUsername());

        if (Boolean.FALSE.equals(user.getIsEmailConfirmed())) {
            log.warn(LogConstants.EMAIL_NOT_CONFIRMED, user.getUsername());
            throw new EmailNotConfirmedException(user.getEmail());
        }

        if (passwordEncoder.matches(changeForgottenPasswordRequest.getNewPassword(), user.getPassword())) {
            log.warn(LogConstants.SAME_PASSWORD, user.getUsername());
            throw new InvalidPasswordException(ExceptionConstants.SAME_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(changeForgottenPasswordRequest.getNewPassword()));
        userRepository.save(user);

        forgotPassword.setStatus(ForgotPasswordStatus.SUCCESS);
        forgotPasswordRepository.save(forgotPassword);

        log.info(LogConstants.PASSWORD_CHANGE_SUCCESS, user.getUsername());
    }

    public void verifyForgotPasswordToken(UUID token) {
        forgotPasswordRepository
                .findByForgotPasswordTokenAndStatus(token, ForgotPasswordStatus.PENDING)
                .orElseThrow(InvalidTokenException::new);
    }
}

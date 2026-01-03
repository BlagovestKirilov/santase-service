package bg.deck.santaseservice.service;

import bg.deck.santaseservice.exception.EmailAlreadyExistsException;
import bg.deck.santaseservice.exception.InvalidCredentialsException;
import bg.deck.santaseservice.exception.InvalidTokenException;
import bg.deck.santaseservice.exception.UserAlreadyExistsException;
import bg.deck.santaseservice.model.Player;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.request.LoginRequest;
import bg.deck.santaseservice.model.request.RegisterRequest;
import bg.deck.santaseservice.model.response.AuthResponse;
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

import static bg.deck.santaseservice.constant.Constants.REAL_IP;
import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_LOGIN;
import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_REFRESH_TOKEN;
import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_REGISTER;
import static bg.deck.santaseservice.constant.LogConstants.SUCCESSFUL_LOGIN_LOG;
import static bg.deck.santaseservice.constant.LogConstants.SUCCESSFUL_REGISTER_LOG;
import static bg.deck.santaseservice.constant.LogConstants.TRY_LOGIN_LOG;
import static bg.deck.santaseservice.constant.LogConstants.TRY_REFRESH_TOKEN;
import static bg.deck.santaseservice.constant.LogConstants.TRY_REGISTER_LOG;

@Log4j2
@Transactional
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest loginRequest, HttpServletRequest request) {
        log.info(TRY_LOGIN_LOG, loginRequest.getUsername());

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .filter(foundUser -> passwordEncoder.matches(loginRequest.getPassword(), foundUser.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(loginRequest.getUsername()));

        log.info(SUCCESSFUL_LOGIN_LOG, loginRequest.getUsername());

        user.setIpAddress(request.getHeader(REAL_IP));
        userRepository.save(user);

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(SUCCESSFUL_LOGIN)
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

        Player player = Player.builder().user(user).build();
        playerRepository.save(player);

        log.info(SUCCESSFUL_REGISTER_LOG, registerRequest.getUsername());

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(SUCCESSFUL_REGISTER)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        log.info(TRY_REFRESH_TOKEN, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        if (username != null && jwtService.isTokenValid(refreshToken)) {

            log.info(SUCCESSFUL_REFRESH_TOKEN);

            return AuthResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .message(SUCCESSFUL_REFRESH_TOKEN)
                    .token(jwtService.generateToken(user))
                    .refreshToken(jwtService.generateRefreshToken(user))
                    .build();
        }

        throw new InvalidTokenException();
    }
}

package bg.deck.santaseservice.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_LOGIN;
import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_REFRESH_TOKEN;
import static bg.deck.santaseservice.constant.Constants.SUCCESSFUL_REGISTER;
import static bg.deck.santaseservice.enums.Role.ROLE_USER;

@Transactional
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .filter(foundUser -> passwordEncoder.matches(loginRequest.getPassword(), foundUser.getPassword()))
                .orElseThrow(() -> new InvalidCredentialsException(loginRequest.getUsername()));

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(SUCCESSFUL_LOGIN)
                .token(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new UserAlreadyExistsException(registerRequest.getUsername());
        }

        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(ROLE_USER);
        userRepository.save(user);

        Player player = Player.builder().user(user).build();
        playerRepository.save(player);

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message(SUCCESSFUL_REGISTER)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        if (username != null && jwtService.isTokenValid(refreshToken)) {
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

package bg.deck.santaseservice;

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
import bg.deck.santaseservice.service.AuthService;
import bg.deck.santaseservice.service.JwtService;
import bg.deck.santaseservice.util.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private final String username = "testUser";
    private final String password = "password123";
    @Mock
    private UserRepository userRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtService jwtService;
    @InjectMocks
    private AuthService authService;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername(username);
        testUser.setPassword("encodedPassword");
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {
        @Test
        void login_Success() {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(password, testUser.getPassword())).thenReturn(true);
            when(jwtService.generateToken(testUser)).thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh-token");

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals(HttpStatus.OK.getReasonPhrase(), response.getStatus());
            assertEquals("access-token", response.getToken());
            verify(userRepository).findByUsername(username);
        }

        @Test
        void login_InvalidCredentials_ThrowsException() {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("wrong-password");

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong-password", testUser.getPassword())).thenReturn(false);

            assertThrows(InvalidCredentialsException.class, () -> authService.login(request));
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {
        @Test
        void register_Success() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);
            request.setPassword(password);

            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode(any())).thenReturn("encodedPassword");

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals(HttpStatus.OK.getReasonPhrase(), response.getStatus());
            verify(userRepository).save(any(User.class));
            verify(playerRepository).save(any(Player.class));
        }

        @Test
        void register_UserExists_ThrowsException() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername(username);

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));

            assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Refresh Token Tests")
    class RefreshTokenTests {
        @Test
        void refreshToken_Success() {
            String oldRefreshToken = "valid-refresh-token";
            when(jwtService.extractUsername(oldRefreshToken)).thenReturn(username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid(oldRefreshToken)).thenReturn(true);
            when(jwtService.generateToken(testUser)).thenReturn("new-access-token");
            when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh-token");

            AuthResponse response = authService.refreshToken(oldRefreshToken);

            assertEquals("new-access-token", response.getToken());
            assertEquals("new-refresh-token", response.getRefreshToken());
        }

        @Test
        void refreshToken_InvalidToken_ThrowsException() {
            String invalidToken = "invalid-token";
            when(jwtService.extractUsername(invalidToken)).thenReturn(username);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(testUser));
            when(jwtService.isTokenValid(invalidToken)).thenReturn(false);

            assertThrows(InvalidTokenException.class, () -> authService.refreshToken(invalidToken));
        }
    }
}

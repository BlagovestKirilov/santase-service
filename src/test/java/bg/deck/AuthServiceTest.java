package bg.deck;

import bg.deck.exception.InvalidCredentialsException;
import bg.deck.exception.InvalidTokenException;
import bg.deck.exception.UserAlreadyExistsException;
import bg.deck.enums.GameType;
import bg.deck.model.EmailConfirmation;
import bg.deck.model.User;
import bg.deck.model.request.LoginRequest;
import bg.deck.model.request.RegisterRequest;
import bg.deck.model.response.AuthResponse;
import bg.deck.repository.EmailConfirmationRepository;
import bg.deck.repository.ForgotPasswordRepository;
import bg.deck.repository.PlayerRepository;
import bg.deck.repository.UserRepository;
import bg.deck.service.AuthService;
import bg.deck.service.EmailService;
import bg.deck.service.JwtService;
import bg.deck.util.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static bg.deck.constant.Constants.CF_CONNECTING_IP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private ForgotPasswordRepository forgotPasswordRepository;
    @Mock
    private EmailConfirmationRepository emailConfirmationRepository;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private JwtService jwtService;
    @Mock
    private HttpServletRequest httpServletRequest;
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
            // given
            LoginRequest request = new LoginRequest(username, password);

            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(password, testUser.getPassword()))
                    .thenReturn(true);
            when(jwtService.generateToken(testUser))
                    .thenReturn("access-token");
            when(jwtService.generateRefreshToken(testUser))
                    .thenReturn("refresh-token");
            when(httpServletRequest.getHeader(CF_CONNECTING_IP))
                    .thenReturn("127.0.0.1");

            // when
            AuthResponse response = authService.login(request, httpServletRequest);

            // then
            assertNotNull(response);
            assertEquals(HttpStatus.OK.getReasonPhrase(), response.status());
            assertEquals("access-token", response.token());
            assertEquals("refresh-token", response.refreshToken());
            assertEquals("127.0.0.1", testUser.getIpAddress());

            verify(userRepository).findByUsername(username);
            verify(userRepository).save(testUser);
            verify(jwtService).generateToken(testUser);
            verify(jwtService).generateRefreshToken(testUser);
        }

        @Test
        void login_InvalidCredentials_ThrowsException() {
            // given
            LoginRequest request = new LoginRequest(username, "wrong-password");

            when(userRepository.findByUsername(username))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrong-password", testUser.getPassword()))
                    .thenReturn(false);

            // then
            assertThrows(
                    InvalidCredentialsException.class,
                    () -> authService.login(request, httpServletRequest)
            );

            verify(userRepository).findByUsername(username);
            verify(userRepository, never()).save(any());
            verify(jwtService, never()).generateToken(any());
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {
        @Test
        void register_Success() {
            RegisterRequest request = new RegisterRequest(username, password, "test@example.com");

            testUser.setPassword(password);
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail("test@example.com")).thenReturn(false);
            when(userMapper.toEntity(request)).thenReturn(testUser);
            when(passwordEncoder.encode(password)).thenReturn("encodedPassword");

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals(HttpStatus.OK.getReasonPhrase(), response.status());
            assertEquals("encodedPassword", testUser.getPassword());
            verify(userRepository, atLeastOnce()).save(testUser);

            // One stats row per game type is created up front.
            assertNotNull(testUser.statsFor(GameType.SANTASE));
            assertNotNull(testUser.statsFor(GameType.TABLA));

            // A pending confirmation is stored for the new user and emailed.
            ArgumentCaptor<EmailConfirmation> confirmation = ArgumentCaptor.forClass(EmailConfirmation.class);
            verify(emailConfirmationRepository).save(confirmation.capture());
            assertEquals(testUser, confirmation.getValue().getUser());
            verify(emailService).sendConfirmationEmail(confirmation.getValue());

            // Seats are created per game, not at registration.
            verifyNoInteractions(playerRepository);
        }

        @Test
        void register_UserExists_ThrowsException() {
            RegisterRequest request = new RegisterRequest(username, null, null);

            when(userRepository.existsByUsername(username)).thenReturn(true);

            assertThrows(UserAlreadyExistsException.class, () -> authService.register(request));
            verify(userRepository, never()).save(any());
            verifyNoInteractions(emailConfirmationRepository, emailService);
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

            assertEquals("new-access-token", response.token());
            assertEquals("new-refresh-token", response.refreshToken());
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

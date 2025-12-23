package com.bussiness.santaseservice.service;

import com.bussiness.santaseservice.model.Player;
import com.bussiness.santaseservice.model.User;
import com.bussiness.santaseservice.model.request.LoginRequest;
import com.bussiness.santaseservice.model.request.RegisterRequest;
import com.bussiness.santaseservice.model.response.AuthResponse;
import com.bussiness.santaseservice.repository.PlayerRepository;
import com.bussiness.santaseservice.repository.UserRepository;
import com.bussiness.santaseservice.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.bussiness.santaseservice.enums.Role.ROLE_USER;

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
                .orElseThrow(() -> new RuntimeException("Username or password is incorrect"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new RuntimeException("Username or password is incorrect");
        }

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message("Successful login")
                .token(jwtService.generateToken(user))
                .refreshToken(jwtService.generateRefreshToken(user))
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole(ROLE_USER);
        userRepository.save(user);

        Player player = Player.builder().user(user).build();
        playerRepository.save(player);

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message("Successful registration")
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username = jwtService.extractUsername(refreshToken);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Username or password is incorrect"));

        if (username != null && jwtService.isTokenValid(refreshToken)) {
            return AuthResponse.builder()
                    .status(HttpStatus.OK.getReasonPhrase())
                    .message("Successful token refresh")
                    .token(jwtService.generateToken(user))
                    .refreshToken(jwtService.generateRefreshToken(user))
                    .build();
        }
        throw new RuntimeException("Invalid Refresh Token");
    }
}

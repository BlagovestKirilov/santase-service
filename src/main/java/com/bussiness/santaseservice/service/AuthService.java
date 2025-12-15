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

@Transactional
@RequiredArgsConstructor
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthResponse login(LoginRequest loginRequest) {

        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("Username or password is incorrect"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return AuthResponse.builder()
                    .status(HttpStatus.BAD_REQUEST.getReasonPhrase())
                    .message("Username or password is incorrect")
                    .build();
        }

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message("Successful login")
                .build();
    }

    public AuthResponse register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already in use");
        }

        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        Player player = Player.builder().user(user).build();
        playerRepository.save(player);

        return AuthResponse.builder()
                .status(HttpStatus.OK.getReasonPhrase())
                .message("Successful registration")
                .build();
    }
}

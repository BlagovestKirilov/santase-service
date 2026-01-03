package bg.deck.santaseservice.service;

import bg.deck.santaseservice.exception.InvalidCredentialsException;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.response.ProfileResponse;
import bg.deck.santaseservice.repository.UserRepository;
import bg.deck.santaseservice.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static bg.deck.santaseservice.constant.LogConstants.TRY_GET_PROFILE;

@Log4j2
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final GameUtilService gameUtilService;
    private final UserMapper userMapper;

    public ProfileResponse getProfile() {
        String username = gameUtilService.getUsername();

        log.info(TRY_GET_PROFILE, username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException(username));

        return userMapper.toProfileResponse(user);
    }
}

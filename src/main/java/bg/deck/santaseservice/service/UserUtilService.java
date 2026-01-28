package bg.deck.santaseservice.service;

import bg.deck.santaseservice.model.DeletedUser;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.repository.DeletedUserRepository;
import bg.deck.santaseservice.repository.EmailConfirmationRepository;
import bg.deck.santaseservice.repository.ForgotPasswordRepository;
import bg.deck.santaseservice.repository.PlayerRepository;
import bg.deck.santaseservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class UserUtilService {
    private final EmailConfirmationRepository emailConfirmationRepository;
    private final DeletedUserRepository deletedUserRepository;
    private final PlayerRepository playerRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final UserRepository userRepository;

    public void deleteUser(User user) {
        DeletedUser deletedUser = DeletedUser.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole())
                .ipAddress(user.getIpAddress())
                .santaseWins(user.getSantaseWins())
                .santaseLosses(user.getSantaseLosses())
                .rank(user.getRank())
                .rankRating(user.getRankRating())
                .email(user.getEmail())
                .isEmailConfirmed(user.getIsEmailConfirmed())
                .deletedAt(Instant.now())
                .build();

        deletedUserRepository.save(deletedUser);

        playerRepository.findByUserUsername(user.getUsername())
                .ifPresent(player -> {
                    player.setDeletedUser(deletedUser);
                    player.setUser(null);
                    playerRepository.save(player);
                });

        emailConfirmationRepository.findByUserUsername(user.getUsername())
                .ifPresent(emailConfirmation -> {
                    emailConfirmation.setDeletedUser(deletedUser);
                    emailConfirmation.setUser(null);
                    emailConfirmationRepository.save(emailConfirmation);
                });

        List<ForgotPassword> forgotPasswords = forgotPasswordRepository.findAllByUser(user);
        for (ForgotPassword forgotPassword : forgotPasswords) {
            forgotPassword.setDeletedUser(deletedUser);
            forgotPassword.setUser(null);
        }
        forgotPasswordRepository.saveAll(forgotPasswords);

        userRepository.delete(user);
    }
}

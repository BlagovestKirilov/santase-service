package bg.deck.service;

import bg.deck.model.DeletedUser;
import bg.deck.model.EmailConfirmation;
import bg.deck.model.ForgotPassword;
import bg.deck.model.Player;
import bg.deck.model.User;
import bg.deck.model.UserDeletion;
import bg.deck.repository.DeletedUserRepository;
import bg.deck.repository.EmailConfirmationRepository;
import bg.deck.repository.ForgotPasswordRepository;
import bg.deck.repository.PlayerRepository;
import bg.deck.repository.UserDeletionRepository;
import bg.deck.repository.UserRepository;
import bg.deck.util.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@RequiredArgsConstructor
@Service
public class UserUtilService {
    private final EmailConfirmationRepository emailConfirmationRepository;
    private final DeletedUserRepository deletedUserRepository;
    private final PlayerRepository playerRepository;
    private final ForgotPasswordRepository forgotPasswordRepository;
    private final UserDeletionRepository userDeletionRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public void deleteUser(User user) {
        DeletedUser deletedUser = userMapper.toDeletedUser(user);
        deletedUserRepository.save(deletedUser);

        // A user owns one player row per game played, so every one of them has to
        // be re-pointed at the tombstone, not just the first.
        List<Player> seats = playerRepository.findAllByUserUsername(user.getUsername());
        seats.forEach(player -> {
            player.setDeletedUser(deletedUser);
            player.setUser(null);
        });
        playerRepository.saveAll(seats);

        List<EmailConfirmation> emailConfirmations = emailConfirmationRepository.findAllByUser(user);
        emailConfirmations.forEach(emailConfirmation -> {
            emailConfirmation.setDeletedUser(deletedUser);
            emailConfirmation.setUser(null);
        });
        emailConfirmationRepository.saveAll(emailConfirmations);

        List<ForgotPassword> forgotPasswords = forgotPasswordRepository.findAllByUser(user);
        forgotPasswords.forEach(forgotPassword -> {
            forgotPassword.setDeletedUser(deletedUser);
            forgotPassword.setUser(null);
        });
        forgotPasswordRepository.saveAll(forgotPasswords);

        List<UserDeletion> userDeletions = userDeletionRepository.findAllByUser(user);
        userDeletions.forEach(userDeletion -> {
            userDeletion.setDeletedUser(deletedUser);
            userDeletion.setUser(null);
        });
        userDeletionRepository.saveAll(userDeletions);

        userRepository.delete(user);
    }
}

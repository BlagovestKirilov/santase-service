package bg.deck.santaseservice.service;

import bg.deck.santaseservice.model.DeletedUser;
import bg.deck.santaseservice.model.EmailConfirmation;
import bg.deck.santaseservice.model.ForgotPassword;
import bg.deck.santaseservice.model.User;
import bg.deck.santaseservice.model.UserDeletion;
import bg.deck.santaseservice.repository.DeletedUserRepository;
import bg.deck.santaseservice.repository.EmailConfirmationRepository;
import bg.deck.santaseservice.repository.ForgotPasswordRepository;
import bg.deck.santaseservice.repository.PlayerRepository;
import bg.deck.santaseservice.repository.UserDeletionRepository;
import bg.deck.santaseservice.repository.UserRepository;
import bg.deck.santaseservice.util.UserMapper;
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

        playerRepository.findByUserUsername(user.getUsername())
                .ifPresent(player -> {
                    player.setDeletedUser(deletedUser);
                    player.setUser(null);
                    playerRepository.save(player);
                });

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

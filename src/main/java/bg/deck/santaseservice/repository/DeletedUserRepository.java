package bg.deck.santaseservice.repository;

import bg.deck.santaseservice.model.DeletedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedUserRepository extends JpaRepository<DeletedUser, Integer> {
}

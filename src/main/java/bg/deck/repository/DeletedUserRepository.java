package bg.deck.repository;

import bg.deck.model.DeletedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeletedUserRepository extends JpaRepository<DeletedUser, Integer> {
}

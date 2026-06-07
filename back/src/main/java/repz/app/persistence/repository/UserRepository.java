package repz.app.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import repz.app.persistence.entity.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByDeletedAtIsNull();

    Optional<User> findByIdAndDeletedAtIsNull(Long id);

    List<User> findByActiveTrueAndDeletedAtIsNull();

    List<User> findByActiveAndDeletedAtIsNull(Boolean active);
}


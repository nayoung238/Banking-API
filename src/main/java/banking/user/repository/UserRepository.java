package banking.user.repository;

import banking.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId);

    @Query("select u from User u join u.accounts a where u.id = :userId and a.id = :accountId")
    Optional<User> findByIdAndAccountId(@Param("userId") Long userId, @Param("accountId") Long accountId);

    @Query("select u from User u join u.accounts a where a.id = :accountId")
    Optional<User> findByAccountId(@Param("accountId") Long accountId);
}

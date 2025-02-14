package banking.users.repository;

import banking.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {

    boolean existsByLoginId(String loginId);
    Optional<Users> findByLoginId(String loginId);

    @Query("select u from Users u join u.accounts a where u.id = :userId and a.id = :accountId")
    Optional<Users> findByIdAndAccountId(@Param("userId") Long userId, @Param("accountId") Long accountId);

    @Query("select u from Users u join u.accounts a where a.id = :accountId")
    Optional<Users> findByAccountId(@Param("accountId") Long accountId);
}

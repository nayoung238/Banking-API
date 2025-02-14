package banking.account.repository;

import banking.account.entity.Account;
import banking.user.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :accountId")
    Optional<Account> findByIdWithLock(@Param("accountId") Long id);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUser(User user);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);

    @Query("select case when exists " +
        "(select 1 from Account a where a.id = :accountId and a.user.id = :userId) then true else false end")
    boolean existsByAccountIdAndUserId(@Param("accountId") Long accountId, @Param("userId") Long userId);
}

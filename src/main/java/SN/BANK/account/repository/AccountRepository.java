package SN.BANK.account.repository;

import SN.BANK.account.entity.Account;
import SN.BANK.users.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id=:accountId")
    Optional<Account> findByIdWithPessimisticLock(@Param("accountId") Long id);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUser(Users user);

    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber=:accountNumber")
    Optional<Account> findByAccountNumberWithPessimisticLock(@Param("accountNumber") String accountNumber);
}

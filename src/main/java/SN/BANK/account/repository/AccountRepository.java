package SN.BANK.account.repository;

import SN.BANK.account.entity.Account;
import SN.BANK.users.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByUser(Users user);
    Optional<Account> findByAccountNumber(String accountNumber);

    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByAccountNumberWithLock(@Param("accountNumber") String accountNumber);
}

package SN.BANK.account.repository;

import SN.BANK.account.entity.Account;
import SN.BANK.users.entity.Users;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :accountId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findByIdWithLock(Long accountId);

    @Query("SELECT a FROM Account a JOIN FETCH a.user WHERE a.id = :accountId")
    Optional<Account> findById(Long accountId);

    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByUser(Users user);
}
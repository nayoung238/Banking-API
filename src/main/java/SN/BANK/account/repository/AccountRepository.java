package SN.BANK.account.repository;

import SN.BANK.account.entity.Account;
import SN.BANK.users.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    List<Account> findByUser(Users user);
}
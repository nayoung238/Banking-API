package SN.BANK.payment.tempRepository;

import SN.BANK.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TempAccountRepository extends JpaRepository<Account,Long> {

    List<Account> findAllByUser_Id(Long userId);
}

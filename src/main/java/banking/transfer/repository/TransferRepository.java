package banking.transfer.repository;

import banking.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findAllByWithdrawalAccountId(Long withdrawalAccountId);
    List<Transfer> findAllByDepositAccountId(Long depositAccountId);
}

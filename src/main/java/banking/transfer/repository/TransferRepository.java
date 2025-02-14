package banking.transfer.repository;

import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findAllByWithdrawalAccountId(Long withdrawalAccountId);
    List<Transfer> findAllByDepositAccountId(Long depositAccountId);
    boolean existsByTransferGroupId(String transferGroupId);
    List<Transfer> findAllByTransferGroupId(String transferGroupId);
    Optional<Transfer> findByTransferGroupIdAndTransferType(String transferGroupId, TransferType transferType);
}

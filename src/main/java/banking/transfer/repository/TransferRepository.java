package banking.transfer.repository;

import banking.transfer.entity.Transfer;
import banking.transfer.enums.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    boolean existsByTransferGroupId(String transferGroupId);

    @Query("SELECT t FROM Transfer t WHERE t.transferGroupId " +
        "= (SELECT t2.transferGroupId FROM Transfer t2 WHERE t2.id = :transferId)")
    List<Transfer> findTransferGroupByTransferId(Long transferId);

    Optional<Transfer> findByTransferGroupIdAndTransferType(String transferGroupId, TransferType transferType);

    @Query("SELECT t FROM Transfer t WHERE t.transferOwnerId = :userId " +
            "AND (t.withdrawalAccountId = :accountId OR t.depositAccountId = :accountId)")
    List<Transfer> findTransfersByUserIdAndAccountId(@Param("userId") Long userId, @Param("accountId") Long accountId);
}

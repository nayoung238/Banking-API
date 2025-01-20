package SN.BANK.transaction.repository;

import SN.BANK.transaction.entity.TransactionEntity;
import SN.BANK.transaction.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    boolean existsByGroupId(String groupId);
    List<TransactionEntity> findBySenderAccountIdAndType(Long senderAccountId, TransactionType type);
    List<TransactionEntity> findByReceiverAccountIdAndType(Long receiverAccountId, TransactionType type);
}

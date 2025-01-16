package SN.BANK.transaction.repository;

import SN.BANK.transaction.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    boolean existsByGroupId(String groupId);
}

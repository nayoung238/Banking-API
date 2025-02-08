package SN.BANK.transfer.repository;

import SN.BANK.transfer.entity.Transfer;
import SN.BANK.transfer.enums.TransferType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {
    boolean existsByGroupId(String groupId);
    List<Transfer> findBySenderAccountIdAndType(Long senderAccountId, TransferType type);
    List<Transfer> findByReceiverAccountIdAndType(Long receiverAccountId, TransferType type);
}

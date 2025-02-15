package banking.transfer.repository;

import banking.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findAllByTransferOwnerId(Long transferOwnerId);
    boolean existsByTransferGroupId(String transferGroupId);
    List<Transfer> findAllByTransferGroupId(String transferGroupId);
    Optional<Transfer> findByTransferGroupIdAndTransferOwnerId(String transferGroupId, Long transferOwnerId);
}

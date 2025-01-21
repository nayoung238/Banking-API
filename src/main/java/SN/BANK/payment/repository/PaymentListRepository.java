package SN.BANK.payment.repository;

import SN.BANK.payment.entity.PaymentList;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentListRepository extends JpaRepository<PaymentList,Long> {

    @Query("SELECT p FROM PaymentList p WHERE p.id = :paymentId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentList> findByIdWithLock(@Param("paymentId") Long paymentId);
}

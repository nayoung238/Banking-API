package SN.BANK.payment.repository;

import SN.BANK.payment.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("SELECT p FROM payment p WHERE p.id = :paymentId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findByIdWithLock(@Param("paymentId") Long paymentId);
}

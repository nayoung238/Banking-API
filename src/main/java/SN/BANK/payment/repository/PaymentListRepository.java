package SN.BANK.payment.repository;

import SN.BANK.payment.entity.PaymentList;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentListRepository extends JpaRepository<PaymentList, Long> {
}

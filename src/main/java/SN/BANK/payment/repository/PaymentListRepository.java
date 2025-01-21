package SN.BANK.payment.repository;

import SN.BANK.payment.entity.PaymentList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentListRepository extends JpaRepository<PaymentList,Long> {
}

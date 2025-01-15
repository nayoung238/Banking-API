package SN.BANK.payment.repository;

import SN.BANK.payment.entity.PaymentList;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentListRepository extends JpaRepository<PaymentList,Long> {

    List<PaymentList> findAllByWithdrawIdIn(List<Long> withdrawIds);
}

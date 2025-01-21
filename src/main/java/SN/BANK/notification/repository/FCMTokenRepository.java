package SN.BANK.notification.repository;

import SN.BANK.notification.entity.FCMToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FCMTokenRepository extends JpaRepository<FCMToken, Long> {
    boolean existsByToken(String token);

}

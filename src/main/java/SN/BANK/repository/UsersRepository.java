package SN.BANK.repository;

import SN.BANK.domain.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsersRepository extends JpaRepository<Users, Long> {
    boolean existsByLoginId(String loginId);
    Optional<Users> findByLoginId(String loginId);
}

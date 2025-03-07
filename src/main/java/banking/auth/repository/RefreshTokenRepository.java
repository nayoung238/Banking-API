package banking.auth.repository;

import banking.auth.entity.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long> {

	boolean existsByRefreshToken(String refreshToken);
}

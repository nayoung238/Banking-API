package banking.common.jwt;

import banking.user.entity.Role;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;

public class TestJwtUtil {

	private static final String SECRET_KEY = "cPlFdV3BdHxqABIwNpgvyOkZgskfj23jeJNxjujgQoE="; // 테스트용 비밀키
	private static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15; // 15분

	public static String generateTestAccessToken(Long userId, Role role) {
		return Jwts.builder()
			.setSubject(userId.toString())
			.claim("userId", userId)
			.claim("role", role.name())
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION))
			.signWith(SignatureAlgorithm.HS256, SECRET_KEY)
			.compact();
	}
}

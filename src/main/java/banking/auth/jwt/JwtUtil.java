package banking.auth.jwt;

import banking.auth.repository.RefreshTokenRepository;
import banking.auth.service.AuthService;
import banking.common.config.JwtAuthenticationFilter;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

	public static final long ACCESS_TOKEN_EXPIRATION = 1000 * 60 * 15;
	private static final long REFRESH_TOKEN_EXPIRATION = 1000 * 60 * 60 * 24 * 7;

	private final JwtProperties jwtProperties;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;

	public String generateAccessToken(Long userId, Role role) {
		return generateToken(userId, role, ACCESS_TOKEN_EXPIRATION);
	}

	public String generateRefreshToken(Long userId, Role role) {
		return generateToken(userId, role, REFRESH_TOKEN_EXPIRATION);
	}

	private String generateToken(Long userId, Role role, long expirationTime) {
		if (userId == null) {
			throw new CustomException(ErrorCode.MISSING_USER_ID);
		}
		if (role == null) {
			throw new CustomException(ErrorCode.MISSING_ROLE);
		}

		return Jwts.builder()
			.setIssuer(jwtProperties.getIssuer())
			.setSubject(userId.toString())
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + expirationTime))
			.claim("userId", userId)
			.claim("role", role.name())
			.signWith(SignatureAlgorithm.HS256, jwtProperties.getSecretKey())
			.compact();
	}

	public String refreshAccessToken(String refreshToken) {
		if (!validateToken(refreshToken)) {
			throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		boolean isExist = refreshTokenRepository.existsByRefreshToken(refreshToken);
		if (!isExist) {
			throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
		}

		try {
			Long userId = getUserIdFromToken(refreshToken);
			Role role = getRole(userId);
			return generateAccessToken(userId, role);
		} catch (CustomException e) {
			if (e.getErrorCode().equals(ErrorCode.TOKEN_EXPIRED)) {
				throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
			}
			throw e;
		}
	}

	public Long getUserIdFromToken(String token) {
		try {
			Claims claims = getClaims(token);
			return claims.get("userId", Long.class);
		} catch (ExpiredJwtException e) {
			throw new CustomException(ErrorCode.TOKEN_EXPIRED);
		} catch (JwtException e) {
			throw new CustomException(ErrorCode.JWT_PROCESSING_FAILED);
		}
	}

	private Role getRole(Long userId) {
		User user = userRepository.findById(userId)
			.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));

		return user.getRole();
	}

	public boolean validateToken(String token) {
		try {
			getClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public Claims getClaims(String token) {
		return Jwts.parser()
			.setSigningKey(jwtProperties.getSecretKey())
			.parseClaimsJws(token)
			.getBody();
	}

	public String getAccessToken(HttpServletRequest httpServletRequest) {
		String header = httpServletRequest.getHeader(JwtAuthenticationFilter.HEADER_AUTHORIZATION);
		if (header != null && header.startsWith(JwtAuthenticationFilter.TOKEN_PREFIX)) {
			return header.split(" ", 2)[1];
		}
		return null;
	}

	public String getRefreshToken(HttpServletRequest httpServletRequest) {
		Cookie[] cookies = httpServletRequest.getCookies();
		if (cookies == null) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (cookie.getName().equals(AuthService.COOKIE_REFRESH_TOKEN_NAME)) {
				return cookie.getValue();
			}
		}
		return null;
	}
}
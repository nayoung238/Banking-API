package banking.auth.service;

import banking.auth.dto.request.LoginRequest;
import banking.auth.entity.RefreshToken;
import banking.auth.jwt.JwtUtil;
import banking.auth.repository.AccessTokenBlacklistRepository;
import banking.auth.repository.RefreshTokenRepository;
import banking.common.config.JwtAuthenticationFilter;
import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.entity.User;
import banking.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserService userService;
	private final JwtUtil jwtUtil;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AccessTokenBlacklistRepository accessTokenBlacklistRepository;

	public final static String COOKIE_REFRESH_TOKEN_NAME = "refresh_token";

	public void login(LoginRequest loginRequest, HttpServletResponse httpServletResponse) {
		User user = userService.findUserEntity(loginRequest.loginId());
		user.verifyPasswordMatching(loginRequest.password());

		String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getRole());
		String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole());

		refreshTokenRepository.save(RefreshToken.of(user.getId(), refreshToken));

		httpServletResponse.setHeader(JwtAuthenticationFilter.HEADER_AUTHORIZATION,
									JwtAuthenticationFilter.TOKEN_PREFIX + accessToken);

		Cookie cookie = new Cookie(COOKIE_REFRESH_TOKEN_NAME, refreshToken);
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		cookie.setPath("/");
		cookie.setMaxAge(60 * 60 * 24 * 7);
		httpServletResponse.addCookie(cookie);
	}

	public void refreshAccessToken(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		String refreshToken = jwtUtil.getRefreshToken(httpServletRequest);
		if (refreshToken == null) {
			throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
		}

		String accessToken = jwtUtil.refreshAccessToken(refreshToken);
		httpServletResponse.setHeader(JwtAuthenticationFilter.HEADER_AUTHORIZATION,
									JwtAuthenticationFilter.TOKEN_PREFIX + accessToken);
	}

	public void logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		String refreshToken = jwtUtil.getRefreshToken(httpServletRequest);
		if (refreshToken != null) {
			Long userId = jwtUtil.getUserIdFromToken(refreshToken);
			refreshTokenRepository.deleteById(userId);
		}

		String accessToken = jwtUtil.getAccessToken(httpServletRequest);
		if (accessToken != null) {
			accessTokenBlacklistRepository.addToBlackList(accessToken, JwtUtil.ACCESS_TOKEN_EXPIRATION);
		}

		Cookie cookie = new Cookie(COOKIE_REFRESH_TOKEN_NAME, null);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		httpServletResponse.addCookie(cookie);
	}
}

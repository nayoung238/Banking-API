package banking.auth.api;

import banking.auth.dto.request.LoginRequest;
import banking.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "로그인", description = "바디에 {loginId, password}을 json 형식으로 보내주세요.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 틀립니다.", content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse httpServletResponse){
		authService.login(loginRequest, httpServletResponse);
		return ResponseEntity.ok("로그인 성공");
	}

	@Operation(summary = "액세스 토큰 재발급")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "액세스 토큰 재발급 성공", content = @Content(schema = @Schema(implementation = String.class))),
		@ApiResponse(responseCode = "401", description = "리프래시 토큰 만료.", content = @Content(schema = @Schema(implementation = String.class)))
	})
	@PostMapping("/refresh")
	public ResponseEntity<?> refresh(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		authService.refreshAccessToken(httpServletRequest, httpServletResponse);
		return ResponseEntity.ok("엑세스 토큰 재발급 완료!");
	}

	@Operation(summary = "로그아웃", description = "액세스, 리프레시 토큰이 제거됩니다.")
	@ApiResponses({
		@ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = String.class)))
	})
	@GetMapping("/logout")
	public ResponseEntity<?> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
		authService.logout(httpServletRequest, httpServletResponse);
		return ResponseEntity.ok("로그아웃 성공");
	}
}

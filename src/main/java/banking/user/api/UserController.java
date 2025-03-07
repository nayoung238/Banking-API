package banking.user.api;

import banking.auth.entity.UserPrincipal;
import banking.user.dto.request.UserCreationRequest;
import banking.user.dto.response.UserResponse;
import banking.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "유저 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입", description = "바디에 {name, loginId, password}을 json 형식 추가 & 회원 가입 성공 시 유저의 DB PK 반환")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "중복된 아이디", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserCreationRequest userCreationRequest) {
        UserResponse response = userService.register(userCreationRequest);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @Operation(summary = "회원정보 가져오기", description = "Request Header에 Access Token 설정")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원정보 가져오기 성공", content = @Content(schema = @Schema(implementation = UserCreationRequest.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findUserDetails(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        UserResponse response = userService.findUserDetails(userPrincipal.getId());
        return ResponseEntity.ok(response);
    }
}

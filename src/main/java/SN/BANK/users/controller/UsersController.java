package SN.BANK.users.controller;

import SN.BANK.users.dto.LoginRequestDto;
import SN.BANK.users.dto.UserCreationRequestDto;
import SN.BANK.users.dto.UserResponseDto;
import SN.BANK.users.entity.CustomUserDetails;
import SN.BANK.users.service.UsersService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
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
public class UsersController {

    private final UsersService userService;

    @Operation(summary = "회원가입" ,description = "바디에 {name, loginId, password}을 json 형식으로 보내주세요. 성공 시 가입 된 회원의 데이터베이스 아이디 값이 보내집니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(schema = @Schema(implementation = Long.class))),
        @ApiResponse(responseCode = "400", description = "중복된 아이디입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserCreationRequestDto userCreationRequestDto){
        UserResponseDto response = userService.register(userCreationRequestDto);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }

    @Operation(summary = "회원정보 가져오기", description = "세션에 연결되어 있어야합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원정보 가져오기 성공", content = @Content(schema = @Schema(implementation = UserCreationRequestDto.class))),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 유저입니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping
    public ResponseEntity<?> findUserDetails(@AuthenticationPrincipal CustomUserDetails customUserDetails){;
        UserResponseDto response = userService.findUserDetails(customUserDetails.getUser().getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그인", description = "바디에 {loginId, password}을 json 형식으로 보내주세요.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = String.class))),
        @ApiResponse(responseCode = "401", description = "아이디 또는 비밀번호가 틀립니다.", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(HttpSession session, @Valid@RequestBody LoginRequestDto loginRequestDto){
        Long userId = userService.login(loginRequestDto);
        session.setAttribute("user", userId);
        return ResponseEntity.ok("로그인 성공");
    }


    @Operation(summary = "로그아웃", description = "세션에 연결되어 있어야합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session){
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }
}

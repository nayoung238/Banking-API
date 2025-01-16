package SN.BANK.users.controller;

import SN.BANK.users.dto.LoginDto;
import SN.BANK.users.dto.UsersRequestDto;
import SN.BANK.users.dto.UsersResponseDto;
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

@Tag(name="Users", description = "유저 API")
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/users")
public class UsersController {
    private final UsersService usersService;

    @Operation(summary = "회원가입",description = "바디에 {name, loginId, password}을 json 형식으로 보내주세요. 성공 시 가입 된 회원의 데이터베이스 아이디 값이 보내집니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201",description = "회원가입 성공",content = @Content(schema = @Schema(implementation = Long.class)))
            ,@ApiResponse(responseCode = "400",description = "중복된 아이디입니다.",content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("")
    public ResponseEntity<Long> createUser(@Valid @RequestBody UsersRequestDto usersRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(usersService.join(usersRequestDto));
    }

    @Operation(summary = "로그인",description = "바디에 {loginId, password}을 json 형식으로 보내주세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그인 성공",content = @Content(schema = @Schema(implementation = String.class)))
            ,@ApiResponse(responseCode = "401",description = "아이디 또는 비밀번호가 틀립니다.",content = @Content(schema = @Schema(implementation = String.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid@RequestBody LoginDto loginDto, HttpSession session){
        Long userId = usersService.checkLogin(loginDto);
        session.setAttribute("user",userId);
        return ResponseEntity.ok("로그인 성공");
    }

    @Operation(summary = "회원정보 가져오기",description = "세션에 연결되어 있어야합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "회원정보 가져오기 성공",content = @Content(schema = @Schema(implementation = UsersRequestDto.class)))
            ,@ApiResponse(responseCode = "404",description = "존재하지 않는 유저입니다.",content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("")
    public ResponseEntity<UsersResponseDto> getUser(@AuthenticationPrincipal CustomUserDetails customUserDetails){;
        return ResponseEntity.ok(usersService.getUserInformation(customUserDetails.getUsers().getId()));
    }

    @Operation(summary = "로그아웃",description = "세션에 연결되어 있어야합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200",description = "로그아웃 성공",content = @Content(schema = @Schema(implementation = String.class)))
    })
    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session){
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }
}

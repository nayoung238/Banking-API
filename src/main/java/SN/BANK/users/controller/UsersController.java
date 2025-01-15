package SN.BANK.users.controller;

import SN.BANK.users.dto.LoginDto;
import SN.BANK.users.dto.UsersRequestDto;
import SN.BANK.users.dto.UsersResponseDto;
import SN.BANK.users.entity.CustomUserDetails;
import SN.BANK.users.service.UsersService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@RequestMapping("/users")
public class UsersController {
    private final UsersService usersService;

    @PostMapping("")
    public ResponseEntity<Long> createUser(@Valid @RequestBody UsersRequestDto usersRequestDto){
        return ResponseEntity.status(HttpStatus.CREATED).body(usersService.join(usersRequestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid@RequestBody LoginDto loginDto, HttpSession session){
        Long userId = usersService.checkLogin(loginDto);
        session.setAttribute("user",userId);
        return ResponseEntity.ok("로그인 성공");
    }

    @GetMapping("")
    public ResponseEntity<UsersResponseDto> getUser(@AuthenticationPrincipal CustomUserDetails customUserDetails){;
        return ResponseEntity.ok(usersService.getUserInformation(customUserDetails.getUsers().getId()));
    }

    @GetMapping("/logout")
    public ResponseEntity<String> logout(HttpSession session){
        session.invalidate();
        return ResponseEntity.ok("로그아웃 성공");
    }
}

package SN.BANK.controller;

import SN.BANK.dto.LoginDto;
import SN.BANK.dto.UsersRequestDto;
import SN.BANK.dto.UsersResponseDto;
import SN.BANK.service.UsersService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<UsersResponseDto> getUser(HttpSession session){
        Long userId = (Long) session.getAttribute("user");
        return ResponseEntity.ok(usersService.getUserInformation(userId));
    }
}

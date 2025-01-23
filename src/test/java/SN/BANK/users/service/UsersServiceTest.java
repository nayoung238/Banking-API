package SN.BANK.users.service;


import SN.BANK.common.exception.CustomException;
import SN.BANK.users.entity.Users;
import SN.BANK.users.dto.LoginDto;
import SN.BANK.users.dto.UsersRequestDto;
import SN.BANK.users.dto.UsersResponseDto;
import SN.BANK.users.repository.UsersRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {
    @InjectMocks
    UsersService usersService;

    @Mock
    UsersRepository usersRepository;
    @Mock
    BCryptPasswordEncoder bCryptPasswordEncoder;


    /*@Test
    @DisplayName("회원가입 성공 테스트")
    public void joinSuccessTest(){
        //given
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        Users user = createUser(name,loginId,password);
        when(usersRepository.existsByLoginId(loginId)).thenReturn(false);
        when(bCryptPasswordEncoder.encode(password)).thenReturn("encodedPassword");
        when(usersRepository.save(any())).thenReturn(user);

        //when
        Long id = usersService.join(usersRequestDto);

        //then
        assertEquals(id,1L);
        verify(usersRepository,times(1)).existsByLoginId(loginId);
        verify(bCryptPasswordEncoder,times(1)).encode(password);
        verify(usersRepository,times(1)).save(any());
    }

    @Test
    @DisplayName("회원가입 실패 테스트 - 중복된 아이디")
    public void joinFailTest(){
        //given
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        UsersRequestDto usersRequestDto = UsersRequestDto.builder().name(name).loginId(loginId).password(password).build();
        when(usersRepository.existsByLoginId(loginId)).thenReturn(true);

        //when
        CustomException exception = assertThrows(CustomException.class,()->usersService.join(usersRequestDto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"중복된 아이디입니다.");
    }*/

    @Test
    @DisplayName("유저 정보 가져오기 성공 테스트")
    public void getUserTest(){
        //given
        Long userId  = 1L;
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        Users user = createUser(name,loginId,password);
        when(usersRepository.findById(userId)).thenReturn(Optional.ofNullable(user));

        //when
        UsersResponseDto usersResponseDto = usersService.getUserInformation(userId);

        assertAll(
                ()->assertEquals(usersResponseDto.getId(),userId),
                ()->assertEquals(usersResponseDto.getLoginId(),loginId),
                ()->assertEquals(usersResponseDto.getName(),name)
        );
        verify(usersRepository,times(1)).findById(userId);
    }

    @Test
    @DisplayName("유저 정보 가져오기 실패 테스트 - 존재하지 않는 유저 id")
    public void getUserFailTest(){
        //given
        Long userId  = 1L;
        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,()->usersService.getUserInformation(userId));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"존재하지 않는 유저입니다.");
        verify(usersRepository,times(1)).findById(userId);
    }

    @Test
    @DisplayName("유저 정보 가져오기 실패 테스트 - 유저 id가 null일 경우")
    public void getUserFailNullTest(){
        //given
        Long userId  = null;

        //when
        CustomException exception = assertThrows(CustomException.class,()->usersService.getUserInformation(userId));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"존재하지 않는 유저입니다.");
    }

    /*@Test
    @DisplayName("로그인 성공 테스트")
    public void loginTest(){
        //given
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        Users user = createUser(name,loginId,password);
        when(usersRepository.findByLoginId(loginId)).thenReturn(Optional.ofNullable(user));
        when(bCryptPasswordEncoder.matches(any(),any())).thenReturn(true);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();

        //when
        Long userId = usersService.checkLogin(loginDto);

        //then
        assertEquals(userId,1L);
        verify(usersRepository,times(1)).findByLoginId(loginId);
        verify(bCryptPasswordEncoder,times(1)).matches(any(),any());
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 틀린 아이디")
    public void loginFailIdTest(){
        //given
        String loginId = "testId";
        String password = "testPassword";
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        when(usersRepository.findByLoginId(loginId)).thenReturn(Optional.empty());

        //when
        CustomException exception = assertThrows(CustomException.class,()->usersService.checkLogin(loginDto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"아이디 또는 비밀번호가 틀립니다.");
    }

    @Test
    @DisplayName("로그인 실패 테스트 - 틀린 비밀번호")
    public void loginFailPasswordTest(){
        //given
        String name = "테스트이름";
        String loginId = "testId";
        String password = "testPassword";
        Users user = createUser(name,loginId,password);
        LoginDto loginDto = LoginDto.builder().loginId(loginId).password(password).build();
        when(usersRepository.findByLoginId(loginId)).thenReturn(Optional.ofNullable(user));
        when(bCryptPasswordEncoder.matches(any(),any())).thenReturn(false);

        //when
        CustomException exception = assertThrows(CustomException.class,()->usersService.checkLogin(loginDto));

        //then
        assertEquals(exception.getErrorCode().getMessage(),"아이디 또는 비밀번호가 틀립니다.");
    }*/


    private Users createUser(String name, String loginId, String password){
        Users user = Users.builder().name(name).loginId(loginId).password(password).build();
        ReflectionTestUtils.setField(user,"id",1L);
        return user;
    }

}

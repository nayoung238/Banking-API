package banking.users.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.users.repository.UsersRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceUnitTest {

    @InjectMocks
    UsersService userService;

    @Mock
    UsersRepository usersRepository;

    @Test
    @DisplayName("[유저 조회 실패 테스트] 유저 존재하지 않으면 NOT_FOUND_USER 에러 코드 예외 발생")
    public void find_user_details_when_user_dose_not_exist_test (){
        // given
        final Long userId  = 1L;
        when(usersRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
		Assertions.assertThatThrownBy(() -> userService.findUserDetails(userId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.NOT_FOUND_USER, customException.getErrorCode());
				assertEquals(HttpStatus.NOT_FOUND, customException.getErrorCode().getStatus());
				assertEquals("존재하지 않는 유저입니다.", customException.getErrorCode().getMessage());
			});
    }

    @Test
    @DisplayName("[유저 조회 실패 테스트] 요청 시 유저 아이디를 null 값이면 NULL_PARAMETER 에러 코드 예외 발생")
    public void find_user_details_when_user_id_null_test () {
        // given
        Long userId  = null;

		// when & then
		Assertions.assertThatThrownBy(() -> userService.findUserDetails(userId))
			.isInstanceOf(CustomException.class)
			.satisfies(ex -> {
				CustomException customException = (CustomException) ex;
				assertEquals(ErrorCode.NULL_PARAMETER, customException.getErrorCode());
				assertEquals(HttpStatus.BAD_REQUEST, customException.getErrorCode().getStatus());
				assertEquals("파라미터가 null 입니다.", customException.getErrorCode().getMessage());
			});
    }
}

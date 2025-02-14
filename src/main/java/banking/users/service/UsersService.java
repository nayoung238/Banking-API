package banking.users.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.users.dto.response.UserPublicInfoDto;
import banking.users.entity.Role;
import banking.users.entity.Users;
import banking.users.dto.LoginRequestDto;
import banking.users.dto.UserCreationRequestDto;
import banking.users.dto.UserResponseDto;
import banking.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final UsersRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public UserResponseDto register(UserCreationRequestDto userCreationRequestDto){
        if(userRepository.existsByLoginId(userCreationRequestDto.loginId())){
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        //String encodedPassword = bCryptPasswordEncoder.encode(usersRequestDto.getPassword());

        try {
            Users user = Users.builder()
                .name(userCreationRequestDto.name())
                .loginId(userCreationRequestDto.loginId())
                .password(userCreationRequestDto.password())
                .role(Role.ROLE_USER)
                .build();

            userRepository.save(user);
            return UserResponseDto.of(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    public UserResponseDto findUserDetails(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.NULL_PARAMETER);
        }
        Users user = findUserEntity(userId);
        return UserResponseDto.of(user);
    }

    public Users findUserEntity(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    public boolean isExistUser(Long userId) {
        return userRepository.existsById(userId);
    }

    public Long login(LoginRequestDto loginRequestDto) {
        Users user = userRepository.findByLoginId(loginRequestDto.loginId())
            .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAIL));

        /*if (!bCryptPasswordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAIL);
        }*/

        if (!user.getPassword().equals(loginRequestDto.password())) {
            throw new CustomException(ErrorCode.LOGIN_FAIL);
        }
        return user.getId();
    }

    public UserPublicInfoDto findUserPublicInfo(Long userId, Long accountId) {
        Users user = userRepository.findByIdAndAccountId(userId, accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoDto.of(user);
    }

    public UserPublicInfoDto findUserPublicInfo(Long accountId) {
        Users user = userRepository.findByAccountId(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoDto.of(user);
    }
}

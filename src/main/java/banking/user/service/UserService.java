package banking.user.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.dto.response.UserPublicInfoDto;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.dto.request.LoginRequestDto;
import banking.user.dto.request.UserCreationRequestDto;
import banking.user.dto.response.UserResponseDto;
import banking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public UserResponseDto register(UserCreationRequestDto userCreationRequestDto){
        if(userRepository.existsByLoginId(userCreationRequestDto.loginId())){
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        //String encodedPassword = bCryptPasswordEncoder.encode(usersRequestDto.getPassword());

        try {
            User user = User.builder()
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
        User user = findUserEntity(userId);
        return UserResponseDto.of(user);
    }

    public User findUserEntity(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    public boolean isExistUser(Long userId) {
        return userRepository.existsById(userId);
    }

    public Long login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByLoginId(loginRequestDto.loginId())
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
        User user = userRepository.findByIdAndAccountId(userId, accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoDto.of(user);
    }

    public UserPublicInfoDto findUserPublicInfo(Long accountId) {
        User user = userRepository.findByAccountId(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoDto.of(user);
    }
}

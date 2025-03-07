package banking.user.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.dto.response.UserPublicInfoResponse;
import banking.user.entity.Role;
import banking.user.entity.User;
import banking.user.dto.request.UserCreationRequest;
import banking.user.dto.response.UserResponse;
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
    public UserResponse register(UserCreationRequest userCreationRequest){
        if(userRepository.existsByLoginId(userCreationRequest.loginId())){
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }

        //String encodedPassword = bCryptPasswordEncoder.encode(usersRequestDto.getPassword());

        try {
            User user = User.builder()
                .name(userCreationRequest.name())
                .loginId(userCreationRequest.loginId())
                .password(userCreationRequest.password())
                .role(Role.USER)
                .build();

            userRepository.save(user);
            return UserResponse.of(user);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
    }

    public UserResponse findUserDetails(Long userId) {
        if (userId == null) {
            throw new CustomException(ErrorCode.NULL_PARAMETER);
        }
        User user = findUserEntity(userId);
        return UserResponse.of(user);
    }

    public User findUserEntity(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    public User findUserEntity(String loginId) {
        return userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_USER));
    }

    public boolean isExistUser(Long userId) {
        return userRepository.existsById(userId);
    }

    public UserPublicInfoResponse findUserPublicInfo(Long userId, Long accountId) {
        User user = userRepository.findByIdAndAccountId(userId, accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoResponse.of(user);
    }

    public UserPublicInfoResponse findUserPublicInfo(Long accountId) {
        User user = userRepository.findByAccountId(accountId)
            .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND_ACCOUNT));

        return UserPublicInfoResponse.of(user);
    }
}

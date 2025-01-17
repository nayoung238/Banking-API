package SN.BANK.users.service;

import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.entity.Users;
import SN.BANK.users.dto.LoginDto;
import SN.BANK.users.dto.UsersRequestDto;
import SN.BANK.users.dto.UsersResponseDto;
import SN.BANK.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UsersService {
    private final UsersRepository usersRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public Long join(UsersRequestDto usersRequestDto){
        if(usersRepository.existsByLoginId(usersRequestDto.getLoginId())){
            throw new CustomException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        String encodedPassword = bCryptPasswordEncoder.encode(usersRequestDto.getPassword());
        Users user = usersRepository.save(Users.builder().name(usersRequestDto.getName()).loginId(usersRequestDto.getLoginId()).password(encodedPassword).build());
        return user.getId();
    }

    public UsersResponseDto getUserInformation(Long userId){
        if(userId==null){
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        Users user = usersRepository.findById(userId).orElseThrow(()->new CustomException(ErrorCode.NOT_FOUND_USER));
        return UsersResponseDto.of(user);
    }

    public Long checkLogin(LoginDto loginDto){
        Users user = usersRepository.findByLoginId(loginDto.getLoginId()).orElseThrow(()->new CustomException(ErrorCode.LOGIN_FAIL));
        if(!bCryptPasswordEncoder.matches(loginDto.getPassword(),user.getPassword())){
            throw new CustomException(ErrorCode.LOGIN_FAIL);
        }
        return user.getId();
    }
}

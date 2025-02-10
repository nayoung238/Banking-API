package banking.users.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.users.entity.CustomUserDetails;
import banking.users.entity.Users;
import banking.users.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findById(Long.valueOf(username))
            .orElseThrow(()-> new CustomException(ErrorCode.NOT_FOUND_USER));

        return new CustomUserDetails(user);
    }
}

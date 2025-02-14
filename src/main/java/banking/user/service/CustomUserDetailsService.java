package banking.user.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.user.entity.CustomUserDetails;
import banking.user.entity.User;
import banking.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findById(Long.valueOf(username))
            .orElseThrow(()-> new CustomException(ErrorCode.NOT_FOUND_USER));

        return new CustomUserDetails(user);
    }
}

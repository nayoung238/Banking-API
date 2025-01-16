package SN.BANK.common.config;

import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.users.service.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class SessionAuthenticationFilter extends OncePerRequestFilter {
    private final CustomUserDetailsService customUserDetailsService;
    public SessionAuthenticationFilter(CustomUserDetailsService customUserDetailsService){
        this.customUserDetailsService = customUserDetailsService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            Long userId = (Long) request.getSession().getAttribute("user");
            log.info("요청 유저 id :{}",userId);
            if(userId!=null){
                UserDetails loginUser = customUserDetailsService.loadUserByUsername(userId.toString());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
        }
        catch (CustomException e){
            setResponse(response,e.getErrorCode());
        }
    }

    private void setResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(errorCode.getStatus().value());
        response.getWriter().print(errorCode.getMessage());
    }
}

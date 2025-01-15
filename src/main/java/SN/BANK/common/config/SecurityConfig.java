package SN.BANK.common.config;

import SN.BANK.common.exception.CustomAccessDeniedHandler;
import SN.BANK.common.exception.CustomAuthenticationEntryPoint;
import SN.BANK.users.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomUserDetailsService customUserDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception{
        httpSecurity.
                csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session->session.maximumSessions(1));
        httpSecurity
                .authorizeHttpRequests(auth->auth
                                .requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/api-docs", "/api-docs/**", "/v3/api-docs/**").permitAll()
                                .requestMatchers(HttpMethod.GET,"/users").hasAnyRole("USER","ADMIN")
                                .requestMatchers("/**").permitAll()
                        );
        httpSecurity
                .addFilterBefore(new SessionAuthenticationFilter(customUserDetailsService), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception->exception.accessDeniedHandler(new CustomAccessDeniedHandler())
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()));
        return httpSecurity.build();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


}

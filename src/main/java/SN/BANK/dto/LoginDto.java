package SN.BANK.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginDto {
    @NotBlank
    private String loginId;
    @NotBlank
    private String password;

    @Builder
    public LoginDto(String loginId, String password){
        this.loginId = loginId;
        this.password = password;
    }
}

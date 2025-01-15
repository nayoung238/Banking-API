package SN.BANK.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UsersRequestDto {
    @NotBlank
    private String name;
    @NotBlank
    private String loginId;
    @NotBlank
    private String password;

    @Builder
    public UsersRequestDto(String name, String loginId, String password){
        this.name = name;
        this.loginId= loginId;
        this.password = password;
    }

}

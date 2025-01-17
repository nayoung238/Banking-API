package SN.BANK.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 요청 Dto")
@Getter
@NoArgsConstructor
public class UsersRequestDto {
    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    private String name;
    @Schema(description = "로그인 id",example = "abc123")
    @NotBlank
    private String loginId;
    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    private String password;

    @Builder
    public UsersRequestDto(String name, String loginId, String password){
        this.name = name;
        this.loginId= loginId;
        this.password = password;
    }

}

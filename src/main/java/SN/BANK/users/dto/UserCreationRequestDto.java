package SN.BANK.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(description = "회원가입 요청 DTO")
@Builder
public record UserCreationRequestDto (

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    String name,

    @Schema(description = "로그인 ID", example = "abc123")
    @NotBlank(message = "로그인 아이디는 필수입니다.")
    String loginId,

    @Schema(description = "비밀번호", example = "12345")
    @NotBlank(message = "비밀번호는 필수입니다.")
    String password
) { }

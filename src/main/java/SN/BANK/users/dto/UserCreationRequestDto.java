package SN.BANK.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(description = "회원가입 요청 DTO")
@Builder
public record UserCreationRequestDto (

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    String name,

    @Schema(description = "로그인 ID", example = "abc123")
    @NotBlank
    String loginId,

    @Schema(description = "비밀번호", example = "12345")
    @NotBlank
    String password
) { }

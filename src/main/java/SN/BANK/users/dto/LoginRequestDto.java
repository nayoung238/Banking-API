package SN.BANK.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "로그인 요청 DTO")
public record LoginRequestDto (

    @Schema(description = "로그인 ID",example = "abc123")
    @NotBlank
    String loginId,

    @Schema(description = "비밀번호",example = "12345")
    @NotBlank
    String password
) { }

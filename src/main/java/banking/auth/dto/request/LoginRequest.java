package banking.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Schema(description = "로그인 요청 DTO")
@Builder
public record LoginRequest (

    @Schema(description = "로그인 ID", example = "loginId123")
    @NotBlank
    String loginId,

    @Schema(description = "비밀번호", example = "password")
    @NotBlank
    String password
) { }

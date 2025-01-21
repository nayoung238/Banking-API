package SN.BANK.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "토큰 요청 dto")
@Getter
public class TokenRequest
{
    @NotNull
    @Schema(description = "유저 아이디")
    Long userId;

    @NotBlank
    @Schema(description = "토큰")
    String token;

    @Builder
    private TokenRequest(Long userId, String token)
    {
        this.userId = userId;
        this.token = token;
    }
}

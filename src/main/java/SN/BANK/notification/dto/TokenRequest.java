package SN.BANK.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "토큰 요청 dto")
@Getter
public class TokenRequest
{
    @NotBlank
    @Schema(description = "유저 아이디")
    Long userId;

    @NotBlank
    @Schema(description = "토큰")
    String token;
}

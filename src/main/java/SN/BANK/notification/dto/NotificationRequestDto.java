package SN.BANK.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "알림 요청 dto")
@Getter
public class NotificationRequestDto {

    @NotBlank
    @Schema(description = "알림 제목",example = "입금")
    String title;

    @NotBlank
    @Schema(description = "알림 내용",example = "성공적으로 완료 되었습니다.")
    String message;

    @NotBlank
    @Schema(description = "토큰")
    String token;
}

package SN.BANK.notification.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NotificationRequestDto {
    @NotBlank
    String title;
    @NotBlank
    String message;
    @NotBlank
    String token;
}

package banking.notification.api;

import banking.notification.dto.request.NotificationRequest;
import banking.notification.dto.request.TokenRequest;
import banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/notification")
@Tag(name = "Notification", description = "FCM 알림 관련 API")
public class NotificationController
{
    private final NotificationService notificationService;

    @PostMapping()
    @Operation(summary = "알림 전송", description = "제목과 내용, 대상 토큰을 사용하여 알림을 전송합니다.")
    public ResponseEntity<String> sendNotification(@RequestBody NotificationRequest notificationRequest)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.sendNotification(notificationRequest));
    }
    @PostMapping("/token")
    public ResponseEntity<String> saveToken(@RequestBody TokenRequest tokenRequest) {

        return ResponseEntity.status(HttpStatus.CREATED).body(notificationService.saveToken(tokenRequest));
    }
}

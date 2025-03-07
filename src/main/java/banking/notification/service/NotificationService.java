package banking.notification.service;

import banking.common.exception.CustomException;
import banking.common.exception.ErrorCode;
import banking.common.exception.NotificationException;
import banking.notification.dto.request.NotificationRequest;
import banking.notification.dto.request.TokenRequest;
import banking.notification.entity.FCMToken;
import banking.notification.repository.FCMTokenRepository;
import banking.user.service.UserService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FCMTokenRepository fcmTokenRepository;
    private final UserService userService;

    public String sendNotification(NotificationRequest notificationRequest) {
        Notification notification = Notification.builder()
                .setTitle(notificationRequest.title())
                .setBody(notificationRequest.message())
                .build();

        Message message = Message.builder()
                .setNotification(notification)
                .setToken(notificationRequest.token())
                .build();

        try {
            return FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            String errorCode = String.valueOf(e.getErrorCode());
            if (errorCode.matches("UNREGISTERED|INVALID_ARGUMENT|NOT_FOUND")) {
                // 잘못된 토큰, 등록되지 않은 토큰, 또는 찾을 수 없는 리소스
                throw new NotificationException(ErrorCode.INVALID_TOKEN);
            } else if (errorCode.matches("RESOURCE_EXHAUSTED|UNAVAILABLE")) {
                // FCM 서비스 사용 불가, 할당량 초과
                throw new NotificationException(ErrorCode.FCM_SERVICE_UNAVAILABLE);
            } else {
                // 기타 알 수 없는 오류
                throw new NotificationException(ErrorCode.FCM_UNKNOWN_ERROR);
            }
        }
    }

    @Transactional
    public String saveToken(TokenRequest tokenRequest) {
        if(!userService.isExistUser(tokenRequest.userId())) {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        if(fcmTokenRepository.existsByToken(tokenRequest.token())) {
            throw new NotificationException(ErrorCode.DUPLICATE_TOKEN);
        }

        FCMToken fcmToken = FCMToken.builder()
            .token(tokenRequest.token())
            .userId(tokenRequest.userId())
            .build();

        return fcmTokenRepository.save(fcmToken).getToken();
    }
}
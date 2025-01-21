package SN.BANK.notification.service;

import SN.BANK.common.exception.CustomException;
import SN.BANK.common.exception.ErrorCode;
import SN.BANK.common.exception.NotificationException;
import SN.BANK.notification.dto.NotificationRequestDto;
import SN.BANK.notification.dto.TokenRequest;
import SN.BANK.notification.entity.FCMToken;
import SN.BANK.notification.repository.FCMTokenRepository;
import SN.BANK.users.service.UsersService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)

public class NotificationService {
    private final FCMTokenRepository fcmTokenRepository;
    private final UsersService usersService;
    public String sendNotification(NotificationRequestDto notificationRequestDto) {
        String token=notificationRequestDto.getToken();

        Notification notification = Notification.builder()
                .setTitle(notificationRequestDto.getTitle())
                .setBody(notificationRequestDto.getMessage())
                .build();

        Message message = Message.builder()
                .setNotification(notification)
                .setToken(token)
                .build();

        try {
            return FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            String errorCode = String.valueOf(e.getErrorCode());

            if ("UNREGISTERED".equals(errorCode) || "INVALID_ARGUMENT".equals(errorCode)) {
                throw new NotificationException(ErrorCode.INVALID_TOKEN);
            } else if ("MESSAGING_SERVICE_NOT_AVAILABLE".equals(errorCode) || "QUOTA_EXCEEDED".equals(errorCode)) {
                // 네트워크/서비스 문제
                throw new NotificationException(ErrorCode.FCM_SERVICE_UNAVAILABLE);
            } else {
                throw new NotificationException(ErrorCode.FCM_UNKNOWN_ERROR);
            }
        }
    }
    @Transactional
    public String saveToken(TokenRequest tokenRequest)
    {
        if(usersService.validateUser(tokenRequest.getUserId())==null)
        {
            throw new CustomException(ErrorCode.NOT_FOUND_USER);
        }
        FCMToken fcmToken=FCMToken.builder().token(tokenRequest.getToken()).userId(tokenRequest.getUserId()).build();
        fcmTokenRepository.save(fcmToken);
        return fcmToken.getToken();
    }
}

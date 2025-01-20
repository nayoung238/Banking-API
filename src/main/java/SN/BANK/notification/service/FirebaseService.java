package SN.BANK.notification.service;

import SN.BANK.notification.dto.NotificationRequestDto;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {
    public ResponseEntity<String> sendNotification(NotificationRequestDto notificationRequestDto) {
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
            String response = FirebaseMessaging.getInstance().send(message);
            return ResponseEntity.ok("Notification sent successfully. Message ID: " + response);
        } catch (FirebaseMessagingException e) {
            String errorCode = String.valueOf(e.getErrorCode());

            if ("UNREGISTERED".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token: " + token);

            } else if ("INVALID_ARGUMENT".equals(errorCode)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid token format: " + token);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unknown error occurred: " + e.getMessage());
            }
        }
    }
}

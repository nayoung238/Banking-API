package SN.BANK.notification.service;

import SN.BANK.common.exception.ErrorCode;
import SN.BANK.common.exception.NotificationException;
import SN.BANK.fixture.testEntity.UserFixture;
import SN.BANK.notification.dto.NotificationRequestDto;
import SN.BANK.notification.dto.TokenRequest;
import SN.BANK.notification.entity.FCMToken;
import SN.BANK.notification.repository.FCMTokenRepository;
import SN.BANK.users.entity.Users;
import SN.BANK.users.repository.UsersRepository;
import SN.BANK.users.service.UsersService;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;

//@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

//    @Mock
//    private FCMTokenRepository fcmTokenRepository;
//
//    @Mock
//    private UsersRepository usersRepository;
//
//    @Mock
//    private UsersService usersService;
//
//    @InjectMocks
//    NotificationService notificationService;
//
//    @Test
//    @DisplayName("토큰 생성 완료")
//    void saveToken_ShouldSaveTokenSuccessfully(){
//		Users user = UserFixture.USER_FIXTURE_1.createUser();
//		FCMToken fcmToken = FCMToken.builder().userId(1L).token("token").build();
//
//        TokenRequest tokenRequest = TokenRequest.builder().token("token").userId(1L).build();
//
//        Mockito.when(fcmTokenRepository.save(any(FCMToken.class))).thenReturn(fcmToken);
//        Mockito.when(usersService.findUserEntity(1L)).thenReturn(user);
//
//        String savedToken = notificationService.saveToken(tokenRequest);
//        System.out.println(savedToken);
//        Assertions.assertEquals("token", savedToken);
//    }
//
//    @Test
//    @DisplayName("토큰이 없는 요청")
//    void saveToken_EmptyToken(){
//        TokenRequest tokenRequest = TokenRequest.builder().token("").userId(1L).build();
//
//        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//        Validator validator = factory.getValidator();
//
//        Set<ConstraintViolation<TokenRequest>> violations = validator.validate(tokenRequest);
//
//        Assertions.assertFalse(violations.isEmpty());
//    }
//
//    @Test
//    @DisplayName("중복된 토큰 요청")
//    void saveToken_DuplicateToken(){
//		Users user = UserFixture.USER_FIXTURE_1.createUser();
//
//        TokenRequest tokenRequest = TokenRequest.builder().token("token").userId(1L).build();
//        Mockito.when(usersService.findUserEntity(1L)).thenReturn(user);
//        Mockito.when(fcmTokenRepository.existsByToken("token")).thenReturn(true);
//
//        // Act & Assert: saveToken 호출 시 CustomException 발생
//        NotificationException exception = Assertions.assertThrows(NotificationException.class, () -> {
//            notificationService.saveToken(tokenRequest);
//        });
//
//        Assertions.assertEquals(ErrorCode.DUPLICATE_TOKEN, exception.getErrorCode());
//    }
//
//    @Test
//    @DisplayName("알림 전송 성공")
//    void sendNotification_Success() throws Exception {
//        try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = Mockito.mockStatic(FirebaseMessaging.class)) {
//
//            FirebaseMessaging mockInstance = Mockito.mock(FirebaseMessaging.class);
//            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
//
//            Mockito.when(mockInstance.send(any(Message.class))).thenReturn("mock-message-id");
//
//            // Arrange
//            NotificationRequestDto request = NotificationRequestDto.builder()
//                    .token("validToken")
//                    .title("Test Title")
//                    .message("Test Message")
//                    .build();
//
//            // Act
//            String result = notificationService.sendNotification(request);
//
//            // Assert
//            Assertions.assertEquals("mock-message-id", result); // 결과 값 검증
//        }
//    }
//
//    @Test
//    @DisplayName("잘못된 토큰으로 인한 예외 발생")
//    void sendNotification_InvalidToken() throws Exception {
//        try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = Mockito.mockStatic(FirebaseMessaging.class)) {
//
//            FirebaseMessaging mockInstance = Mockito.mock(FirebaseMessaging.class);
//            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
//
//            FirebaseMessagingException mockException = Mockito.mock(FirebaseMessagingException.class);
//            Mockito.when(mockException.getErrorCode()).thenReturn(com.google.firebase.ErrorCode.INVALID_ARGUMENT);
//            Mockito.when(mockInstance.send(Mockito.any(Message.class)))
//                    .thenThrow(mockException);
//
//            NotificationRequestDto request = NotificationRequestDto.builder()
//                    .token("invalidToken")
//                    .title("Test Title")
//                    .message("Test Message")
//                    .build();
//
//            NotificationException exception = Assertions.assertThrows(NotificationException.class, () -> {
//                notificationService.sendNotification(request);
//            });
//
//            Assertions.assertEquals(ErrorCode.INVALID_TOKEN, exception.getErrorCode());
//        }
//    }
//
//    @Test
//    @DisplayName("FCM서비스 장애로 인한 예외 발생")
//    void sendNotification_fcmServiceUnavailable() throws Exception {
//        try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = Mockito.mockStatic(FirebaseMessaging.class)) {
//
//            FirebaseMessaging mockInstance = Mockito.mock(FirebaseMessaging.class);
//            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
//
//            FirebaseMessagingException mockException = Mockito.mock(FirebaseMessagingException.class);
//            Mockito.when(mockException.getErrorCode()).thenReturn(com.google.firebase.ErrorCode.RESOURCE_EXHAUSTED);
//            Mockito.when(mockInstance.send(Mockito.any(Message.class)))
//                    .thenThrow(mockException);
//
//            NotificationRequestDto request = NotificationRequestDto.builder()
//                    .token("invalidToken")
//                    .title("Test Title")
//                    .message("Test Message")
//                    .build();
//
//            NotificationException exception = Assertions.assertThrows(NotificationException.class, () -> {
//                notificationService.sendNotification(request);
//            });
//
//            Assertions.assertEquals(ErrorCode.FCM_SERVICE_UNAVAILABLE, exception.getErrorCode());
//        }
//    }
//
//    @Test
//    @DisplayName("FCM서비스 장애로 인한 예외 발생")
//    void sendNotification_fcmUnknownError() throws Exception {
//        try (MockedStatic<FirebaseMessaging> firebaseMessagingMock = Mockito.mockStatic(FirebaseMessaging.class)) {
//
//            FirebaseMessaging mockInstance = Mockito.mock(FirebaseMessaging.class);
//            firebaseMessagingMock.when(FirebaseMessaging::getInstance).thenReturn(mockInstance);
//
//            FirebaseMessagingException mockException = Mockito.mock(FirebaseMessagingException.class);
//            Mockito.when(mockException.getErrorCode()).thenReturn(com.google.firebase.ErrorCode.UNKNOWN);
//            Mockito.when(mockInstance.send(Mockito.any(Message.class)))
//                    .thenThrow(mockException);
//
//            NotificationRequestDto request = NotificationRequestDto.builder()
//                    .token("invalidToken")
//                    .title("Test Title")
//                    .message("Test Message")
//                    .build();
//
//            NotificationException exception = Assertions.assertThrows(NotificationException.class, () -> {
//                notificationService.sendNotification(request);
//            });
//
//            Assertions.assertEquals(ErrorCode.FCM_UNKNOWN_ERROR, exception.getErrorCode());
//        }
//    }
}
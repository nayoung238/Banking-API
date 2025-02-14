package banking.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Users
    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST,"이미 사용 중인 로그인 아이디입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED,"아이디 또는 비밀번호가 일치하지 않습니다."),

    // Account
    NOT_FOUND_ACCOUNT(HttpStatus.NOT_FOUND, "존재하지 않는 계좌입니다."),
    NOT_FOUND_WITHDRAWAL_ACCOUNT(HttpStatus.NOT_FOUND, "출금 계좌를 찾을 수 없습니다."),
    NOT_FOUND_DEPOSIT_ACCOUNT(HttpStatus.NOT_FOUND, "입금 계좌를 찾을 수 없습니다."),
    UNAUTHORIZED_ACCOUNT_ACCESS(HttpStatus.FORBIDDEN, "해당 계좌에 대한 접근 권한이 없습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST,"잔액이 부족합니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // Transfer
    SAME_ACCOUNT_TRANSFER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "같은 계좌 간 거래는 불가합니다."),
    NOT_FOUND_TRANSFER(HttpStatus.NOT_FOUND, "존재하지 않는 거래내역입니다."),
    NOT_FOUND_WITHDRAWAL_TRANSFER(HttpStatus.NOT_FOUND, "존재하지 않는 출금 거래입니다."),
    UNAUTHORIZED_TRANSFER_ACCESS(HttpStatus.FORBIDDEN, "해당 이체 내역에 대한 접근 권한이 없습니다."),
    DUPLICATE_TRANSFER_TYPE(HttpStatus.BAD_REQUEST, "이미 존재하는 이체 타입입니다."),
    UNSUPPORTED_TRANSFER_TYPE(HttpStatus.BAD_REQUEST, "지원하지 않는 이체 타입입니다."),
    
    // ExchangeRate
    EXCHANGE_RATE_FETCH_FAIL(HttpStatus.SERVICE_UNAVAILABLE, "환율 데이터를 가져올 수 없습니다."),
    INVALID_QUOTE_CURRENCY_ERROR(HttpStatus.BAD_REQUEST, "Quote Currency는 한국 원화(KRW) 단위여야 합니다."),
    INVALID_EXCHANGE_RATE(HttpStatus.INTERNAL_SERVER_ERROR, "환율 값은 0보다 커야 합니다."),

    // Payment
    PAYMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 결제 취소된 내역입니다."),
    NOT_FOUND_PAYMENT(HttpStatus.BAD_REQUEST,"결제내역이 존재하지 않습니다."),

    // Data
    DECRYPTION_FAIL(HttpStatus.BAD_REQUEST, "복호화에 실패했습니다. 올바른 암호화 키를 사용하고 있는지 확인해주세요."),

    // Notification
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED,"잘못된 토큰입니다."),
    FCM_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE,"네트워크 및 서비스에 장애가 있습니다."),
    FCM_UNKNOWN_ERROR(HttpStatus.INTERNAL_SERVER_ERROR,"알수 없는 에러입니다"),
    DUPLICATE_TOKEN(HttpStatus.BAD_REQUEST,"중복된 토큰입니다."),

    // Valid
    NULL_PARAMETER(HttpStatus.BAD_REQUEST, "파라미터가 null 입니다.");

    private final HttpStatus status;
    private final String message;
}

package SN.BANK.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Users
    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST,"중복된 아이디입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED,"아이디 또는 비밀번호가 틀립니다."),

    // Account
    NOT_FOUND_ACCOUNT(HttpStatus.NOT_FOUND, "존재하지 않는 계좌입니다."),
    UNAUTHORIZED_ACCOUNT_ACCESS(HttpStatus.FORBIDDEN, "해당 계좌에 대한 접근 권한이 없습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),

    // Transfer
    INSUFFICIENT_MONEY(HttpStatus.BAD_REQUEST, "계좌의 잔액이 부족합니다."),
    INVALID_TRANSFER(HttpStatus.BAD_REQUEST, "같은 계좌 간 이체는 불가합니다."),
    NOT_FOUND_TRANSACTION(HttpStatus.NOT_FOUND, "존재하지 않는 거래내역입니다."),

    // ExchangeRate
    EXCHANGE_RATE_FETCH_FAIL(HttpStatus.SERVICE_UNAVAILABLE, "환율 데이터를 가져올 수 없습니다."),

    // Payment
    PAYMENT_ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "이미 결제 취소된 내역입니다.");


    private final HttpStatus status;
    private final String message;
}

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
    UNAUTHORIZED_ACCOUNT_ACCESS(HttpStatus.UNAUTHORIZED, "해당 계좌에 대한 접근 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}

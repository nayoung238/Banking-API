package SN.BANK.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    DUPLICATE_LOGIN_ID(HttpStatus.BAD_REQUEST,"중복된 아이디입니다.");

    private final HttpStatus status;
    private final String message;
}

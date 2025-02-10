package banking.common.exception;


import lombok.Getter;

@Getter
public class NotificationException extends RuntimeException{
    private final ErrorCode errorCode;

    public NotificationException(ErrorCode errorCode){
        this.errorCode = errorCode;
    }
}

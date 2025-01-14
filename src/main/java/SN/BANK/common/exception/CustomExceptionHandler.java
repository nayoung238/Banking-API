package SN.BANK.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class CustomExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<String> MyException(CustomException ex){
        log.error("예외 발생 msg:{}",ex.getErrorCode().getMessage());
        return ResponseEntity.status(ex.getErrorCode().getStatus()).body(ex.getErrorCode().getMessage());
    }
}

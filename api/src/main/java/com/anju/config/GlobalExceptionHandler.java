package com.anju.config;

import com.anju.common.BusinessException;
import com.anju.common.Result;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        Result<Void> result = Result.fail(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(resolveStatus(ex.getCode())).body(result);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));

        if (message.isBlank()) {
            message = "Request parameter validation failed.";
        }
        Result<Void> result = Result.fail(4001, message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolationException(ConstraintViolationException ex) {
        Result<Void> result = Result.fail(4002, "Request parameter validation failed.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("Unhandled system exception", ex);
        Result<Void> result = Result.fail(5000, "System is busy, please try again later.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    private HttpStatus resolveStatus(int code) {
        if (code >= 5000) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (code >= 4090 && code < 4100) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 4040 && code < 5000) {
            return HttpStatus.NOT_FOUND;
        }
        if (code >= 4030 && code < 4040) {
            return HttpStatus.FORBIDDEN;
        }
        if (code >= 4010 && code < 4030) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private String formatFieldError(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return fieldError.getField() + " is invalid";
        }
        return fieldError.getField() + ": " + defaultMessage;
    }
}

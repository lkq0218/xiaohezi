package com.xiaohezi.api;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import org.springframework.dao.OptimisticLockingFailureException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
            .findFirst().map(error -> error.getDefaultMessage()).orElse("请求参数不合法");
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiResponse<Void>> malformed(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "请求格式或参数不合法");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Void>> business(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, "BUSINESS_ERROR", exception.getMessage());
    }

    @ExceptionHandler({TimeoutException.class, CompletionException.class})
    ResponseEntity<ApiResponse<Void>> dependency(Exception exception) {
        log.warn("External or asynchronous operation failed", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE", "依赖服务暂时不可用");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiResponse<Void>> conflict(OptimisticLockingFailureException exception) {
        return error(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION",
            "数据已被其他请求更新，请刷新后重试");
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Void>> unknown(Exception exception) {
        log.error("Unhandled request failure", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "系统暂时不可用");
    }

    private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(code, message));
    }
}

package com.app.wooridooribe.exception;

import com.app.wooridooribe.controller.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    /**
     * CustomException 처리 - API 스펙에 맞춘 응답
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(
            CustomException ex, 
            HttpServletRequest request) {
        
        log.error("CustomException: {} - {}", ex.getErrorCode(), ex.getMessage());
        
        ErrorCode errorCode = ex.getErrorCode();
        
        // errorName은 ErrorCode enum의 이름을 사용
        ApiResponse<Void> response = ApiResponse.error(
                errorCode.getStatusCode().value(),
                errorCode.name(),  // "USER_NOT_FOUND", "INVALID_CREDENTIALS" 등
                errorCode.getErrorMsg()  // "아이디가 존재하지 않습니다" 등
        );
        
        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(response);
    }
    
    /**
     * 일반 Exception 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, 
            HttpServletRequest request) {
        
        log.error("Exception: {}", ex.getMessage(), ex);
        
        ApiResponse<Void> response = ApiResponse.error(
                500,
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다."
        );
        
        return ResponseEntity
                .status(500)
                .body(response);
    }
}


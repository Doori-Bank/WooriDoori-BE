package com.app.wooridooribe.exception;

import com.app.wooridooribe.controller.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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
                                errorCode.name(), // "USER_NOT_FOUND", "INVALID_CREDENTIALS" 등
                                errorCode.getErrorMsg() // "아이디가 존재하지 않습니다" 등
                );

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(response);
        }

        /**
         * JSON 파싱 에러 처리
         */
        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {

                String errorMessage = ex.getMessage();
                log.warn("JSON 파싱 에러: {}", errorMessage);

                // "NULL" 문자열이 포함된 경우 명확한 메시지 제공
                if (errorMessage != null && errorMessage.contains("NULL")) {
                        return ResponseEntity
                                        .status(HttpStatus.BAD_REQUEST)
                                        .body(ApiResponse.error(
                                                        HttpStatus.BAD_REQUEST.value(),
                                                        "JSON 파싱 오류: 'NULL' 문자열 대신 null 값을 사용해주세요. 또는 해당 필드를 생략해주세요."));
                }

                // 일반적인 JSON 파싱 에러
                return ResponseEntity
                                .status(HttpStatus.BAD_REQUEST)
                                .body(ApiResponse.error(
                                                HttpStatus.BAD_REQUEST.value(),
                                                "잘못된 JSON 형식입니다. 요청 데이터를 확인해주세요."));
        }

        /**
         * 일반 Exception 처리
         */
        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiResponse<Void>> handleException(
                        Exception ex,
                        HttpServletRequest request) {

                log.error("Exception: {}", ex.getMessage(), ex);

                ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

                ApiResponse<Void> response = ApiResponse.error(
                                errorCode.getStatusCode().value(),
                                errorCode.name(),
                                errorCode.getErrorMsg());

                return ResponseEntity
                                .status(errorCode.getStatusCode())
                                .body(response);
        }
}
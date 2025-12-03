package com.app.wooridooribe.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@Slf4j
public class SseInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // SSE 경로인 경우 헤더 설정
        if (request.getRequestURI().startsWith("/sse/")) {
            // SSE 응답 헤더 설정
            response.setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE);
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("X-Accel-Buffering", "no"); // Nginx 버퍼링 방지
            
            // CORS는 SecurityConfig에서 중앙 관리하므로 여기서는 제거
            // 중복 설정 시 "multiple values" 오류 발생 가능
            
            log.debug("SSE 인터셉터: 헤더 설정 완료 - URI: {}", request.getRequestURI());
        }
        return true;
    }
}


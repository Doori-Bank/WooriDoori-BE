package com.app.wooridooribe.jwt;


import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    // 실제 필터링 로직은 doFilterInternal 에 들어감
    // JWT 토큰의 인증 정보를 현재 쓰레드의 SecurityContext 에 저장하는 역할 수행
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {

        try {
            // 1. Request Header 에서 토큰을 꺼냄
            String jwt = resolveToken(request);

            // 2. 토큰이 있으면 검증 (validateToken이 예외를 던짐)
            if (StringUtils.hasText(jwt)) {
                tokenProvider.validateToken(jwt);  // 실패 시 CustomException 던짐
                // 성공하면 인증 정보 설정
                Authentication authentication = tokenProvider.getAuthentication(jwt);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (CustomException e) {
            // TokenProvider에서 던진 CustomException (TOKEN_EXPIRED, INVALID_TOKEN)
            log.error("JWT 검증 실패: {} - {}", e.getErrorCode().name(), e.getMessage());
            request.setAttribute("jwtErrorCode", e.getErrorCode());
        } catch (Exception e) {
            // 기타 예외
            log.error("JWT 필터 처리 중 예외 발생: {}", e.getMessage());
            request.setAttribute("jwtErrorCode", ErrorCode.INVALID_TOKEN);
        }

        filterChain.doFilter(request, response);
    }

    // Request Header 또는 Cookie에서 토큰 정보를 꺼내오기
    private String resolveToken(HttpServletRequest request) {
        // 1. Authorization 헤더에서 토큰 확인
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        
        // 2. Cookie에서 토큰 확인 (SSE용)
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}
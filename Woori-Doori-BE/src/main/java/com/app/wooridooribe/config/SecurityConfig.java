package com.app.wooridooribe.config;


import com.app.wooridooribe.handler.JwtAccessDeniedHandler;
import com.app.wooridooribe.jwt.JwtAuthenticationEntryPoint;
import com.app.wooridooribe.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final TokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;

    @Value("${DOORIBANK_URL}")
    private String dooriBankUrl;
    
    /**
     * URL의 끝 슬래시 제거 (CORS origin 매칭을 위해)
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // 허용할 오리진 설정
        // Spring Boot 3.x에서는 setAllowedOriginPatterns() 사용 권장 (와일드카드 패턴 지원)
        // frontendUrl이 localhost인 경우 패턴에 포함되므로 중복 제거
        java.util.List<String> allowedOrigins = new java.util.ArrayList<>();
        
        // URL 정규화 (끝 슬래시 제거)
        String normalizedFrontendUrl = normalizeUrl(frontendUrl);
        String normalizedDooriBankUrl = normalizeUrl(dooriBankUrl);
        
        // frontendUrl이 localhost가 아닌 경우만 추가 (프로덕션 환경)
        if (normalizedFrontendUrl != null && !normalizedFrontendUrl.contains("localhost")) {
            allowedOrigins.add(normalizedFrontendUrl);
        }
        
        // dooriBankUrl 추가
        if (normalizedDooriBankUrl != null && !normalizedDooriBankUrl.contains("localhost")) {
            allowedOrigins.add(normalizedDooriBankUrl);
        }
        
        // 로컬 개발 환경 포트 유연하게 처리 (localhost는 패턴으로 처리)
        allowedOrigins.add("http://localhost:*");
        
        configuration.setAllowedOriginPatterns(allowedOrigins);

        // 허용할 HTTP 메서드
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // 허용할 헤더
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 인증 정보 포함 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);
        
        // preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);
        
        // CORS 응답 헤더 노출 설정 (클라이언트에서 접근 가능한 헤더)
        configuration.setExposedHeaders(Arrays.asList("*"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 설정 Disable
                .csrf(AbstractHttpConfigurer::disable)
                
                // CORS 설정 활성화
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                
                // Exception handling 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                
                // H2 콘솔을 위한 설정 (필요시)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                
                // 세션을 사용하지 않기 때문에 STATELESS로 설정
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                
                // 권한별 URL 접근 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // 인증 관련 경로는 모두 허용
                        .requestMatchers("/test/**").permitAll() // 테스트용 경로 (배포 전 삭제 필요!)
                        .requestMatchers("/ws/**").permitAll() // WebSocket 경로 허용
                        .requestMatchers("/sse/test/**").permitAll() // SSE 테스트 경로는 인증 불필요 (Swagger UI 테스트용)
                        .requestMatchers("/sse/**").authenticated() // SSE 경로는 인증 필요
                        .requestMatchers("/files/**").permitAll() // 파일 경로 허용
                        .requestMatchers("/history/calendar/sync").permitAll() // 두리뱅킹 결제 동기화 경로 허용
                        .requestMatchers("/swagger-ui.html","/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll() // Swagger UI
                        .requestMatchers("/actuator/**").permitAll() // Actuator 엔드포인트 허용 (Prometheus 메트릭 수집용)
                        .requestMatchers("/member/**").hasRole("USER") // 나머지 회원 경로는 USER 권한 필요
                        .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자 경로는 ADMIN 권한 필요
                        .anyRequest().authenticated() // 나머지는 인증 필요
                )
                
                // JWT 필터 적용
                .with(new JwtSecurityConfig(tokenProvider), customizer -> {});

        return http.build();
    }
}
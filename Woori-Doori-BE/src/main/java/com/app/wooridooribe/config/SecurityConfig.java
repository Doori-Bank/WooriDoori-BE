package com.app.wooridooribe.config;


import com.app.wooridooribe.handler.JwtAccessDeniedHandler;
import com.app.wooridooribe.jwt.JwtAuthenticationEntryPoint;
import com.app.wooridooribe.jwt.TokenProvider;
import lombok.RequiredArgsConstructor;
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

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final TokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 설정 Disable
                .csrf(AbstractHttpConfigurer::disable)
                
                // CORS 설정
                .cors(AbstractHttpConfigurer::disable)
                
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
                        .requestMatchers("/files/**").permitAll() // 파일 경로 허용
                        .requestMatchers("/swagger-ui.html","/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll() // Swagger UI
                        .requestMatchers("/member/**").hasRole("USER") // 나머지 회원 경로는 USER 권한 필요
                        .requestMatchers("/admin/**").hasRole("ADMIN") // 관리자 경로는 ADMIN 권한 필요
                        .anyRequest().authenticated() // 나머지는 인증 필요
                )
                
                // JWT 필터 적용
                .with(new JwtSecurityConfig(tokenProvider), customizer -> {});

        return http.build();
    }
}
package com.app.wooridooribe.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT Token";
        
        // JWT 인증 스키마 설정
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);
        
        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력하세요 (Bearer 제외)")
                );
        
        return new OpenAPI()
                // 서버 URL 설정 (Nginx 리버스 프록시 환경에서 X-Forwarded-Prefix 헤더와 함께 사용)
                // X-Forwarded-Prefix 헤더가 있으면 Springdoc이 자동으로 경로를 조정하므로
                // 여기서는 상대 경로(/)를 사용하거나 실제 서버 URL을 설정할 수 있습니다
                .addServersItem(new Server().url("http://172.16.1.120:8080").description("Production Server"))
                .addServersItem(new Server().url("/").description("Current Server (with X-Forwarded-Prefix)"))
                .info(new Info()
                        .title("우리두리 API")
                        .description("우리두리 백엔드 REST API 문서")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Woori-Doori Team")
                                .url("https://github.com/Woori-Doori")
                        )
                )
                .addSecurityItem(securityRequirement)
                .components(components);
    }
}


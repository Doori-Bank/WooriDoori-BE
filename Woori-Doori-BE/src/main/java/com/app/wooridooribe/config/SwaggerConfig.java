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
                // 서버 URL 설정 (HTTPS로 명시적으로 설정하여 Mixed Content 오류 방지)
                .addServersItem(new Server().url("https://api.wooridoori.site").description("Production Server (HTTPS)"))
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


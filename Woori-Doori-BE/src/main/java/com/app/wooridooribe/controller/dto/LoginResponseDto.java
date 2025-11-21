package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.Authority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "로그인/회원가입 응답 DTO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {
    
    @Schema(description = "회원 이름", example = "김두리")
    private String name;

    @Schema(description = "회원 권한", example = "USER")
    private Authority authority;

    @Schema(description = "토큰 정보")
    private TokenRequestDto tokens;
}


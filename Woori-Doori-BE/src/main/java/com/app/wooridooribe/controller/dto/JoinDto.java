package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원가입 요청 DTO")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinDto {
    
    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    private String id;
    
    @Schema(description = "비밀번호", example = "password123")
    private String password;
    
    @Schema(description = "회원 이름", example = "김두리")
    private String name;
    
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;
    
    @Schema(description = "생년월일 앞자리", example = "990101")
    private String birthDate;
    
    @Schema(description = "생년월일 뒷자리", example = "1234567")
    private String birthBack;
    
    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .memberId(this.id)
                .password(encodedPassword)
                .memberName(this.name)
                .phone(this.phone)
                .birthDate(this.birthDate)
                .birthBack(this.birthBack)
                .status(StatusType.ABLE)
                .authority(Authority.USER)
                .build();
    }
}


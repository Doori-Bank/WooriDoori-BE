package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinDto {
    
    @JsonProperty("id")
    private String memberId;
    
    private String password;
    
    private String name;
    
    private String phone;
    
    private String birthDate;
    
    private String birthBack;
    
    /**
     * DTO를 Entity로 변환
     * 비밀번호는 암호화된 상태로 넘겨받음
     */
    public Member toEntity(String encodedPassword) {
        return Member.builder()
                .memberId(this.memberId)
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


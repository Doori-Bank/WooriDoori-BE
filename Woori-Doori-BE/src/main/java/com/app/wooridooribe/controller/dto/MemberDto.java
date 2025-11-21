package com.app.wooridooribe.controller.dto;


import com.app.wooridooribe.entity.type.Authority;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private String memberId;
    private String name;
    private String phone;
    private String birthDate;
    private String birthBack;
    private Authority authority;
}
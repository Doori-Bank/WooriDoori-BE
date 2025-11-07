package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "아이디 조회용 DTO (사용자용)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberSearchIdDto {

    @Schema(description = "회원 이름", example = "김두리")
    String name;

    @Schema(description = "회원 전화번호 ", example = "01012341234")
    String phone;

    public static MemberSearchIdDto from(Member member) {
        return MemberSearchIdDto.builder()
                .name(member.getMemberName())
                .phone(member.getPhone())
                .build();
    }
}

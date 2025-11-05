package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDto {
    
    private Long id;
    private String memberId;
    private String memberName;
    private String phone;
    private String birthDate;
    private StatusType status;
    private Authority authority;
    
    /**
     * Entity를 DTO로 변환 (비밀번호 제외)
     */
    public static MemberResponseDto from(Member member) {
        return MemberResponseDto.builder()
                .id(member.getId())
                .memberId(member.getMemberId())
                .memberName(member.getMemberName())
                .phone(member.getPhone())
                .birthDate(member.getBirthDate())
                .status(member.getStatus())
                .authority(member.getAuthority())
                .build();
    }
}


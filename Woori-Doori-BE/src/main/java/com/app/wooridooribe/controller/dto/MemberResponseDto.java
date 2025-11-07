package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원 정보 응답 DTO (관리자용)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberResponseDto {
    
    @Schema(description = "회원 고유 번호", example = "1")
    private Long id;
    
    @Schema(description = "회원 ID (이메일)", example = "test@example.com")
    private String memberId;
    
    @Schema(description = "회원 이름", example = "김두리")
    private String memberName;
    
    @Schema(description = "전화번호", example = "01012345678")
    private String phone;
    
    @Schema(description = "생년월일", example = "990101")
    private String birthDate;
    
    @Schema(description = "회원 상태", example = "ABLE")
    private StatusType status;
    
    @Schema(description = "권한", example = "USER")
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


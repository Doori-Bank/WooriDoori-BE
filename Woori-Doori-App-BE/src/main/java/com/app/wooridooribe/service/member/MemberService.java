package com.app.wooridooribe.service.member;

import com.app.wooridooribe.controller.dto.MemberResponseDto;

import java.util.List;

public interface MemberService {
    
    /**
     * 전체 회원 조회 (관리자용)
     */
    List<MemberResponseDto> getAllMembers();
    
    /**
     * 특정 회원 조회 (관리자용)
     */
    MemberResponseDto getMemberByIdForAdmin(Long memberId);
}

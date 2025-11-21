package com.app.wooridooribe.service.member;

import com.app.wooridooribe.controller.dto.MemberResponseDto;
import com.app.wooridooribe.entity.type.CategoryType;

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

    /**
     * 회원의 필수 카테고리 설정 및 해당 카테고리 결제 내역 미포함 처리
     */
    void updateEssentialCategories(Long memberId, java.util.List<CategoryType> essentialCategories);

    /**
     * 회원 권한 변경 (관리자 전용)
     */
    MemberResponseDto updateMemberAuthority(String memberId, com.app.wooridooribe.entity.type.Authority authority);
}

package com.app.wooridooribe.repository.categoryMember;

import com.app.wooridooribe.entity.type.CategoryType;

import java.util.List;

public interface CategoryMemberQueryDsl {
    
    /**
     * 특정 회원의 필수 카테고리 이름 리스트 조회
     * @param memberId 회원 ID
     * @return 필수 카테고리 이름 리스트
     */
    List<CategoryType> findEssentialCategoryNamesByMemberId(Long memberId);
}


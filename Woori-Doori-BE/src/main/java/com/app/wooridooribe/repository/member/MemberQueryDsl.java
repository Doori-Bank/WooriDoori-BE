package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Member 관련 QueryDSL 커스텀 쿼리 인터페이스
 */
public interface MemberQueryDsl {

    /**
     * 최근 3개월 내에 로그인한 회원 조회
     *
     * @param threeMonthsAgo 기준 시각 (3개월 전)
     * @return 활성 회원 목록
     */
    List<Member> findMembersLoggedInWithinThreeMonths(LocalDateTime threeMonthsAgo);
}



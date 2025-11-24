package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


public interface GoalQueryDsl {
    List<Goal> findGoalsForThisAndNextMonth(Member member);
    
    Optional<Goal> findCurrentMonthGoalByMemberId(Long memberId);
    
    // 특정 시작 날짜의 목표 조회 (지난 달 목표 조회용)
    Optional<Goal> findGoalByMemberIdAndStartDate(Long memberId, LocalDate startDate);

    List<Goal> findAllGoalsByMember(Long memberId);

    List<Goal> findPastGoalsByMember(Long memberId);
}
package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;

import java.util.Optional;

public interface GoalQueryDsl {

    /**
     * 특정 회원의 이번 달 목표를 조회합니다.
     * goalStartDate가 이번 달 1일인 목표를 반환합니다.
     */
    Optional<Goal> findCurrentMonthGoalByMemberId(Long memberId);
}


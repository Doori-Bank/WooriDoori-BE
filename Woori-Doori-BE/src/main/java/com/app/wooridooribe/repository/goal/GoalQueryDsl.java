package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;

import java.util.List;
import java.util.Optional;


public interface GoalQueryDsl {
    List<Goal> findGoalsForThisAndNextMonth(Member member);
    public Optional<Goal> findCurrentMonthGoalByMemberId(Long memberId);
}
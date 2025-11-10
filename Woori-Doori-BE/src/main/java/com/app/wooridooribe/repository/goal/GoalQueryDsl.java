package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;

import java.time.LocalDate;
import java.util.List;

public interface GoalQueryDsl {
    List<Goal> findGoalsForThisAndNextMonth(Member member);

}
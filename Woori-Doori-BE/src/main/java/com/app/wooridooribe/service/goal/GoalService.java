package com.app.wooridooribe.service.goal;


import com.app.wooridooribe.controller.dto.GoalDto;
import com.app.wooridooribe.controller.dto.SetGoalDto;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.jwt.MemberDetail;


public interface GoalService {
    SetGoalDto setGoal(Long memberId, SetGoalDto setGoalDto);

    //GoalDto getGoalHistory(Long goalId);
}

package com.app.wooridooribe.service.goal;


import com.app.wooridooribe.controller.dto.GoalResponseDto;
import com.app.wooridooribe.controller.dto.SetGoalDto;


public interface GoalService {
    GoalResponseDto setGoal(Long memberId, SetGoalDto setGoalDto);
    //GoalDto getGoalHistory(Long goalId);
}

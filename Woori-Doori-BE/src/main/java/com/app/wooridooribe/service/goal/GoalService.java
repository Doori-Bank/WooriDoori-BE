package com.app.wooridooribe.service.goal;


import com.app.wooridooribe.controller.dto.*;

import java.util.List;


public interface GoalService {
    GoalResponseDto setGoal(Long memberId, SetGoalDto setGoalDto);
    
    GoalScoreResponseDto calculateAndUpdateGoalScores(Long memberId);
    
    /**
     * 배치 작업용: 점수 계산만 하고 SSE 알림은 전송하지 않음
     * @param memberId 사용자 ID
     * @return 계산된 점수
     */
    GoalScoreResponseDto calculateAndUpdateGoalScoresBatch(Long memberId);
    
    /**
     * 모든 활성 유저의 점수를 배치로 계산
     * @return 처리된 유저 수
     */
    int calculateAllActiveUsersScores();
    
    DashboardResponseDto getDashboardData(Long memberId);
    ReportResponseDto getReportData(Long memberId);

    DashboardResponseDto getPastGoalData(Long memberId, int year, int month);
    List<GetGoalDto> getGoalHistory(Long memberId);
}

package com.app.wooridooribe.controller.dto;

import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponseDto {
    private boolean thisMonthGoalExists;  // 이번 달 목표 존재 여부
    private boolean nextMonthGoalExists;  // 다음 달 목표 존재 여부
    private SetGoalDto goalData; // 실제 데이터
}

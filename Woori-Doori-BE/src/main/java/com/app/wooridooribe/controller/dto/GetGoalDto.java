package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.type.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

import static com.app.wooridooribe.entity.QGoal.goal;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

@Builder
public class GetGoalDto {

    @Schema(description = "목표 시작 날짜", example = "2025-11-01")
    private LocalDate goalStartDate;

    @Schema(description = "월 수입", example = "2000")
    private String goalIncome; // 수입

    @Schema(description = "목표 제한금액", example = "300")
    private Integer previousGoalMoney; // 목표소비금액

    public static GetGoalDto fromEntity(Goal goal) {
        return GetGoalDto.builder()
                .goalStartDate(goal.getGoalStartDate())
                .goalIncome(goal.getGoalIncome())
                .previousGoalMoney(goal.getPreviousGoalMoney())
                .build();
    }

}
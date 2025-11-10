package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.Authority;
import com.app.wooridooribe.entity.type.JobType;
import com.app.wooridooribe.entity.type.StatusType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor

@Builder
public class SetGoalDto {

    @Schema(description = "회원 직업", example = "EMPLOYEE")
    private JobType goalJob; // 직업

    @Schema(description = "목표 시작 날짜", example = "2025-11-01")
    private LocalDate goalStartDate;

    @Schema(description = "월 수입", example = "2000")
    private String goalIncome; // 수입

    @Schema(description = "목표 제한금액", example = "300")
    private Integer previousGoalMoney; // 목표소비금액

    public Goal toEntity() {
        return Goal.builder()
                .goalJob(this.getGoalJob())
                .goalStartDate(this.getGoalStartDate())
                .goalIncome(this.getGoalIncome())
                .previousGoalMoney(this.getPreviousGoalMoney())
                .build();
    }

}
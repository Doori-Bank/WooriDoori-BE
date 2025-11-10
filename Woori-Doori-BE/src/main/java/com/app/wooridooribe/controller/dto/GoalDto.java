package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.Goal;

import com.app.wooridooribe.entity.type.JobType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class GoalDto {

    @Schema(description = "회원 ID", example = "1L")
    private Long memberId;

    @Schema(description = "목표 시작 날짜", example = "2025-11-01")
    private LocalDate goalStartDate;

    @Schema(description = "목표 제한금액", example = "300")
    private Integer previousGoalMoney;

    @Schema(description = "회원 직업", example = "EMPLOYEE")
    private JobType goalJob;

    @Schema(description = "월 수입", example = "2000")
    private String goalIncome;

}
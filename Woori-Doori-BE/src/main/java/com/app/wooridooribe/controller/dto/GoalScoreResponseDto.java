package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.CategoryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "목표 점수 계산 결과 DTO")
public class GoalScoreResponseDto {
    
    @Schema(description = "목표 달성도 점수 (40점 만점)", example = "35")
    private Integer achievementScore;
    
    @Schema(description = "소비 안정성 점수 (20점 만점)", example = "15")
    private Integer stabilityScore;
    
    @Schema(description = "필수/비필수 비율 점수 (20점 만점)", example = "18")
    private Integer ratioScore;
    
    @Schema(description = "절약 지속성 점수 (20점 만점)", example = "12")
    private Integer continuityScore;
    
    @Schema(description = "총점 (100점 만점)", example = "80")
    private Integer totalScore;
    
    @Schema(description = "카테고리별 소비 금액", example = "{\"CAFE\": 50000, \"FOOD\": 200000, \"SHOPPING\": 150000}")
    private Map<CategoryType, Integer> categorySpending;
}


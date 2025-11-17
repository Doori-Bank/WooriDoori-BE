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
@Schema(description = "대시보드 화면용 응답 DTO")
public class DashboardResponseDto {
    
    @Schema(description = "이번달 목표 금액", example = "120000")
    private Integer goalAmount;
    
    @Schema(description = "이번달 달성률 (0~100)", example = "80")
    private Integer achievementRate;
    
    @Schema(description = "목표 달성도 점수 (40점 만점)", example = "35")
    private Integer achievementScore;
    
    @Schema(description = "소비 안정성 점수 (20점 만점)", example = "15")
    private Integer stabilityScore;
    
    @Schema(description = "필수/비필수 비율 점수 (20점 만점)", example = "18")
    private Integer ratioScore;
    
    @Schema(description = "절약 지속성 점수 (20점 만점)", example = "12")
    private Integer continuityScore;
    
    @Schema(description = "카테고리별 소비 금액 (금액 순으로 정렬된 TOP 4)", 
            example = "{\"FOOD\": 330314, \"TRANSPORT\": 330314, \"SHOPPING\": 330314, \"EDUCATION\": 330314}")
    private Map<CategoryType, Integer> topCategorySpending;
}


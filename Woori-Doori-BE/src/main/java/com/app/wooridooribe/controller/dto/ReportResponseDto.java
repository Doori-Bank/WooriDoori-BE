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
public class ReportResponseDto {
    
    @Schema(description = "이번달 목표 금액", example = "120000")
    private Integer goalAmount;
    
    @Schema(description = "총 지출", example = "8000000")
    private Integer actualSpending;
    
    @Schema(description = "소비점수 (100점 만점)", example = "85")
    private Integer goalScore;
    
    @Schema(description = "카테고리별 소비 금액 (금액 순으로 정렬)",
            example = "{\"FOOD\": 330314, \"TRANSPORT\": 330314, \"SHOPPING\": 330314, \"EDUCATION\": 330314}")
    private Map<CategoryType, Integer> CategorySpending;
}


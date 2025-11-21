package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.util.List;

@Schema(description = "메인 페이지 응답 DTO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MainDto {
    
    @Schema(description = "전체 목표 기간 (일)", example = "30")
    private Integer fullDate;
    
    @Schema(description = "경과 날짜 (일)", example = "20")
    private Integer duringDate;
    
    @Schema(description = "예상 남은 일수 (현재 페이스로 목표 금액을 초과하는 예상 일수)", example = "5")
    private Integer remainingDays;
    
    @Schema(description = "목표 달성률 (%)", example = "80")
    private Integer goalPercent;
    
    @Schema(description = "목표 금액", example = "10000")
    private Integer goalMoney;
    
    @Schema(description = "총 지출 금액", example = "1080000")
    private Integer totalPaidMoney;
    
    @Schema(description = "카테고리별 지출 TOP 5")
    private List<CategorySpendDto> paidPriceOfCategory;
    
    @Schema(description = "추천 카드 TOP 3")
    private List<CardRecommendDto> cardRecommend;
}

package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.CardHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "소비 내역 월별 요약 응답 DTO")
public class CardHistorySummaryResponseDto {
    @Schema(description = "총 지출 금액", example = "156000")
    private int totalAmount;

    @Schema(description = "소비 내역 리스트")
    private List<CardHistory> histories;
}

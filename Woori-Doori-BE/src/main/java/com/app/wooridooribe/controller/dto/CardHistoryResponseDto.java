package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.CardHistory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "소비 내역 상세 응답 DTO")
public class CardHistoryResponseDto {
    @Schema(description = "소비 내역 ID", example = "12")
    private Long id;

    @Schema(description = "소비 일자", example = "2025-11-07")
    private LocalDate date;

    @Schema(description = "소비 내역 이름", example = "스타벅스 강남점")
    private String historyName;

    @Schema(description = "소비 카테고리", example = "카페/간식")
    private String historyCategory;

    @Schema(description = "소비 금액", example = "4500")
    private Integer historyPrice;

    @Schema(description = "더치페이 인원 수", example = "2")
    private Integer historyDutchpay;

    @Schema(description = "1인당 부담 금액", example = "2250")
    private Integer perPersonAmount;

    @Schema(description = "지출 합계 포함 여부", example = "YES")
    private String includeTotal;

    public static CardHistoryResponseDto from(CardHistory entity) {
        int perPersonAmount = entity.getHistoryPrice() / entity.getHistoryDutchpay();

        return CardHistoryResponseDto.builder()
                .id(entity.getId())
                .date(entity.getHistoryDate())
                .historyName(entity.getHistoryName())
                .historyCategory(entity.getHistoryCategory())
                .historyPrice(entity.getHistoryPrice())
                .historyDutchpay(entity.getHistoryDutchpay())
                .perPersonAmount(perPersonAmount)
                .includeTotal(entity.getHistoryIncludeTotal())
                .build();
    }
}

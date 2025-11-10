package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.type.StatusType;
import com.querydsl.core.Tuple;

import java.time.LocalDate;
import java.util.List;

public interface CardHistoryQueryDsl {

    CardHistorySummaryResponseDto findByUserAndMonthAndStatus(Long userId, int year, int month, StatusType status);

    CardHistory findDetailById(Long historyId);

    void updateIncludeTotal(Long historyId, boolean includeTotal);

    void updateCategory(Long historyId, String newCategory);

    void updateDutchpay(Long historyId, int count);

    void updatePrice(Long historyId, int price);

    // 총 지출 금액 조회
    Integer getTotalSpentByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    // 카테고리별 지출 TOP 5 조회
    List<Tuple> getCategorySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    // 가장 많이 사용한 카드 TOP 3 조회
    List<Tuple> getTopUsedCardsByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);
}

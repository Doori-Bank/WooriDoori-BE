package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.StatusType;
import com.querydsl.core.Tuple;

import java.time.LocalDate;
import java.util.List;

public interface CardHistoryQueryDsl {

    CardHistorySummaryResponseDto findByUserAndMonthAndStatus(Long userId, int year, int month, StatusType status);

    CardHistory findDetailById(Long historyId);

    void updateIncludeTotal(Long historyId, boolean includeTotal);

    /**
     * 특정 회원의 결제 내역 중 지정한 카테고리들의 총지출 포함 여부를 일괄 변경
     *
     * @param memberId   회원 ID
     * @param categories 대상 카테고리 목록
     * @param includeTotal true 이면 포함(Y), false 이면 미포함(N)
     */
    void updateIncludeTotalByMemberAndCategories(Long memberId, List<CategoryType> categories, boolean includeTotal);

    void updateCategory(Long historyId, CategoryType newCategory);

    void updateDutchpay(Long historyId, int count);

    void updatePrice(Long historyId, int price);

    // 총 지출 금액 조회
    Integer getTotalSpentByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    // 카테고리별 지출 TOP 5 조회
    List<Tuple> getCategorySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    // 가장 많이 사용한 카드 TOP 3 조회
    List<Tuple> getTopUsedCards();

    // 일별 지출 금액 조회 (소비 안정성 계산용)
    List<Integer> getDailySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate);

    // 필수/비필수 지출 금액 조회 (카테고리 기반)
    List<Tuple> getEssentialNonEssentialSpending(Long memberId, LocalDate startDate, LocalDate endDate, List<CategoryType> essentialCategories);
}

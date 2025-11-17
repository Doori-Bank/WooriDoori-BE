package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import java.time.LocalDate;

import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpendingServiceImpl implements SpendingService {

    private final CardHistoryRepository cardHistoryRepository;

    @Override
    public Map<String, Object> getMonthlySpendings(Long memberId, LocalDate targetDate) {
        if (targetDate == null) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_DATE);
        }

        int year = targetDate.getYear();
        int month = targetDate.getMonthValue();

        CardHistorySummaryResponseDto summary = cardHistoryRepository
                .findByUserAndMonthAndStatus(memberId, year, month, StatusType.ABLE);

        List<CardHistory> histories = summary.getHistories();
        if (histories == null || histories.isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        List<CardHistoryResponseDto> spendingList = histories.stream()
                .map(CardHistoryResponseDto::from)
                .toList();

        // perPersonAmount를 보여주고 싶다면, total은 DB 기준으로
        int totalAmount = summary.getTotalAmount();

        return Map.of(
                "year", year,
                "month", month,
                "totalAmount", totalAmount,
                "spendings", spendingList
        );
    }

    @Override
    public CardHistoryResponseDto getSpendingDetail(Long historyId, Long memberId) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) throw new CustomException(ErrorCode.HISTORY_ISNULL);

        assertOwnership(historyId, memberId);

        return CardHistoryResponseDto.from(entity);
    }

    @Override
    @Transactional
    public void updateIncludeTotal(Long historyId, Long memberId, boolean includeTotal) {
        if (!cardHistoryRepository.existsById(historyId)) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        assertOwnership(historyId, memberId);

        try {
            cardHistoryRepository.updateIncludeTotal(historyId, includeTotal);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_INCLUDE_UPDATE_FAIL);
        }
    }

    @Override
    @Transactional
    public void updateCategory(Long historyId, Long memberId, String newCategory) {
        if (!cardHistoryRepository.existsById(historyId)) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        assertOwnership(historyId, memberId);

        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_CATEGORY);
        }

        // String을 CategoryType으로 변환
        CategoryType categoryType;
        try {
            // ENUM 이름으로 변환 (대소문자 구분 없음)
            categoryType = CategoryType.valueOf(newCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_CATEGORY);
        }

        try {
            cardHistoryRepository.updateCategory(historyId, categoryType);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_CATEGORY_UPDATE_FAIL);
        }
    }

    @Override
    @Transactional
    public void updateDutchpay(Long historyId, Long memberId, int count) {
        if (!cardHistoryRepository.existsById(historyId)) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        assertOwnership(historyId, memberId);

        if (count < 1 || count > 10) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_DUTCHPAY);
        }

        try {
            cardHistoryRepository.updateDutchpay(historyId, count);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_DUTCHPAY_UPDATE_FAIL);
        }
    }

    @Override
    @Transactional
    public void updatePrice(Long historyId, Long memberId, int price) {
        if (!cardHistoryRepository.existsById(historyId)) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        assertOwnership(historyId, memberId);

        if (price <= 0) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_PRICE);
        }

        try {
            cardHistoryRepository.updatePrice(historyId, price);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_PRICE_UPDATE_FAIL);
        }
    }

    private void assertOwnership(Long historyId, Long memberId) {
        boolean mine = cardHistoryRepository.existsByIdAndMemberCardMemberId(historyId, memberId);
        if (!mine) {
            throw new CustomException(ErrorCode.HISTORY_ISNOTYOURS);
        }
    }

}

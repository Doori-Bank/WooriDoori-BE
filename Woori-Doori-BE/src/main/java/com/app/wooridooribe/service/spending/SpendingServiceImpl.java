package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.cardhistory.CardHistoryRepository;
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
    public Map<String, Object> getMonthlySpendings(Long userId, int year, int month) {
        validateDate(year, month);

        CardHistorySummaryResponseDto summary = cardHistoryRepository
                .findByUserAndMonthAndStatus(userId, year, month, StatusType.ABLE);

        // history.isEmpty() 검사 대신 summary 내부에서 합계가 0일 때 판단
        if (summary.getHistories().isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        List<CardHistoryResponseDto> spendingList = summary.getHistories().stream()
                .map(CardHistoryResponseDto::from)
                .toList();

        int totalAmount = spendingList.stream()
                .mapToInt(CardHistoryResponseDto::getPerPersonAmount)
                .sum();

        return Map.of(
                "totalAmount", totalAmount,
                "spendings", spendingList
        );
    }

    private void validateDate(int year, int month) {
        if (year < 2000 || month < 1 || month > 12) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_DATE, ErrorCode.HISTORY_INVALID_DATE.getErrorMsg());
        }
    }

    @Override
    public CardHistoryResponseDto getSpendingDetail(Long historyId) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) throw new CustomException(ErrorCode.HISTORY_ISNULL);
        return CardHistoryResponseDto.from(entity);
    }

    @Override
    @Transactional
    public void updateIncludeTotal(Long historyId, boolean includeTotal) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        try {
            cardHistoryRepository.updateIncludeTotal(historyId, includeTotal);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_INCLUDE_UPDATE_FAIL, ErrorCode.HISTORY_INCLUDE_UPDATE_FAIL.getErrorMsg());
        }
    }

    @Override
    @Transactional
    public void updateCategory(Long historyId, String newCategory) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_CATEGORY, ErrorCode.HISTORY_INVALID_CATEGORY.getErrorMsg());
        }

        try {
            cardHistoryRepository.updateCategory(historyId, newCategory);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_CATEGORY_UPDATE_FAIL, ErrorCode.HISTORY_CATEGORY_UPDATE_FAIL.getErrorMsg());
        }
    }

    @Override
    @Transactional
    public void updateDutchpay(Long historyId, int count) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

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
    public void updatePrice(Long historyId, int price) {
        CardHistory entity = cardHistoryRepository.findDetailById(historyId);
        if (entity == null) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        if (price <= 0) {
            throw new CustomException(ErrorCode.HISTORY_INVALID_PRICE);
        }

        try {
            cardHistoryRepository.updatePrice(historyId, price);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_PRICE_UPDATE_FAIL);
        }
    }
}

package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
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

        var summary = cardHistoryRepository
                .findByUserAndMonthAndStatus(userId, year, month, StatusType.ABLE);

        // history.isEmpty() 검사 대신 summary 내부에서 합계가 0일 때 판단
        if (summary.getHistories().isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_ISNULL);
        }

        List<CardHistoryResponseDto> spendingList = summary.getHistories().stream()
                .map(CardHistoryResponseDto::from)
                .toList();

        return Map.of(
                "totalAmount", summary.getTotalAmount(),
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
        cardHistoryRepository.updateIncludeTotal(historyId, includeTotal);
    }

    @Override
    @Transactional
    public void updateCategory(Long historyId, String newCategory) {
        if (newCategory == null || newCategory.trim().isEmpty()) {
            throw new CustomException(ErrorCode.HISTORY_UPDATE_FAIL, ErrorCode.HISTORY_UPDATE_FAIL.getErrorMsg());
        }

        try {
            cardHistoryRepository.updateCategory(historyId, newCategory);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.HISTORY_UPDATE_FAIL, ErrorCode.HISTORY_UPDATE_FAIL.getErrorMsg());
        }
    }
}

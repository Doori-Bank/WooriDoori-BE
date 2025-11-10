package com.app.wooridooribe.repository.cardhistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.type.StatusType;

public interface CardHistoryQueryDsl {

    CardHistorySummaryResponseDto findByUserAndMonthAndStatus(Long userId, int year, int month, StatusType status);

    CardHistory findDetailById(Long historyId);

    void updateIncludeTotal(Long historyId, boolean includeTotal);

    void updateCategory(Long historyId, String newCategory);

    void updateDutchpay(Long historyId, int count);

    void updatePrice(Long historyId, int price);
}

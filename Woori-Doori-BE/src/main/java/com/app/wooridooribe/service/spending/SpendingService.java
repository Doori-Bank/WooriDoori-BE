package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import java.time.LocalDate;
import java.util.Map;

public interface SpendingService {

    Map<String, Object> getMonthlySpendings(Long memberId, LocalDate targetDate);

    CardHistoryResponseDto getSpendingDetail(Long historyId, Long memberId);

    void updateIncludeTotal(Long historyId, Long memberId, boolean includeTotal);

    void updateCategory(Long historyId, Long memberId, String newCategory);

    void updateDutchpay(Long historyId, Long memberId, int count);

    void updatePrice(Long historyId, Long memberId, int price);
}

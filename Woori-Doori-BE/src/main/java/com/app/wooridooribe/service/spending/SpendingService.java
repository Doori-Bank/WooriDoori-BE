package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import java.util.Map;

public interface SpendingService {

    Map<String, Object> getMonthlySpendings(Long userId, int year, int month);

    CardHistoryResponseDto getSpendingDetail(Long historyId);

    void updateIncludeTotal(Long historyId, boolean includeTotal);
}

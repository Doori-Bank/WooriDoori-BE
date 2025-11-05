package com.app.wooridooribe.service.spending;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import java.util.Map;


public interface SpendingService {

    /**
     * 월별 소비 내역 조회 (캘린더 / 리스트)
     * @param userId 사용자 고유번호
     * @param year 연도
     * @param month 월
     * @return 합계 + 소비 내역 리스트
     */
    Map<String, Object> getMonthlySpendings(Long userId, int year, int month);

    CardHistoryResponseDto getSpendingDetail(Long historyId);
}

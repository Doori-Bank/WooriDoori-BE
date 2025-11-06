package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import com.app.wooridooribe.service.spending.SpendingService;
import com.app.wooridooribe.controller.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/history/calendar")
public class SpendingController {

    private final SpendingService spendingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySpending(
            @RequestParam Long userId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        Map<String, Object> result = spendingService.getMonthlySpendings(userId, year, month);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "소비 내역 조회 성공", result));
    }

    @GetMapping("/detail/{historyId}")
    public ResponseEntity<ApiResponse<CardHistoryResponseDto>> getSpendingDetail(
            @PathVariable Long historyId
    ) {
        CardHistoryResponseDto result = spendingService.getSpendingDetail(historyId);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "소비 내역 상세 조회 성공", result));
    }

    @PatchMapping("/{historyId}/{includeInTotal}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateIncludeTotal(
            @PathVariable Long historyId,
            @PathVariable boolean includeInTotal
    ) {
        spendingService.updateIncludeTotal(historyId, includeInTotal);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "includeInTotal", includeInTotal
        );

        String message = includeInTotal
                ? "지출 합계에 포함되었습니다."
                : "지출 합계에서 제외되었습니다.";

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), message, result)
        );
    }

    @PatchMapping("/{historyId}/category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(
            @PathVariable Long historyId,
            @RequestBody Map<String, String> request
    ) {
        String newCategory = request.get("category");
        spendingService.updateCategory(historyId, newCategory);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newCategory", newCategory
        );

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "카테고리 수정 성공", result)
        );
    }

    @PatchMapping("/{historyId}/dutchpay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDutchpay(
            @PathVariable Long historyId,
            @RequestBody Map<String, Integer> request
    ) {
        int count = request.get("count");
        spendingService.updateDutchpay(historyId, count);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newDutchpayCount", count
        );

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "더치페이 인원 수정 성공", result)
        );
    }

    @PatchMapping("/{historyId}/price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePrice(
            @PathVariable Long historyId,
            @RequestBody Map<String, Integer> request
    ) {
        int price = request.get("price");
        spendingService.updatePrice(historyId, price);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newPrice", price
        );

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "결제 금액 수정 성공", result)
        );
    }

}

package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.spending.SpendingService;
import com.app.wooridooribe.controller.dto.ApiResponse;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/history/calendar")
public class SpendingController {

    private final SpendingService spendingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySpending(
            Authentication authentication,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        Map<String, Object> result = spendingService.getMonthlySpendings(memberId, targetDate);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "조회에 성공하였습니다", result));
    }

    @GetMapping("/detail/{historyId}")
    public ResponseEntity<ApiResponse<CardHistoryResponseDto>> getSpendingDetail(
            Authentication authentication,
            @PathVariable Long historyId
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        CardHistoryResponseDto result = spendingService.getSpendingDetail(historyId, memberId);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "상세 내역 조회에 성공하였습니다", result));
    }

    @PatchMapping("/{historyId}/{includeInTotal}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateIncludeTotal(
            Authentication authentication,
            @PathVariable Long historyId,
            @PathVariable boolean includeInTotal
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        spendingService.updateIncludeTotal(historyId, memberId, includeInTotal);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "includeInTotal", includeInTotal
        );

        String message = includeInTotal
                ? "지출 합계에 포함되었습니다"
                : "지출 합계에서 제외되었습니다";

        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), message, result));
    }

    @PatchMapping("/{historyId}/category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(
            Authentication authentication,
            @PathVariable Long historyId,
            @RequestBody Map<String, String> request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        String newCategory = request.get("category");
        spendingService.updateCategory(historyId, memberId, newCategory);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newCategory", newCategory
        );

        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "성공적으로 수정이 완료되었습니다", result));
    }

    @PatchMapping("/{historyId}/dutchpay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDutchpay(
            Authentication authentication,
            @PathVariable Long historyId,
            @RequestBody Map<String, Integer> request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        int count = request.get("count");
        spendingService.updateDutchpay(historyId, memberId, count);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newDutchpayCount", count
        );

        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "성공적으로 수정이 완료되었습니다", result));
    }

    @PatchMapping("/{historyId}/price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePrice(
            Authentication authentication,
            @PathVariable Long historyId,
            @RequestBody Map<String, Integer> request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        int price = request.get("price");
        spendingService.updatePrice(historyId, memberId, price);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "newPrice", price
        );

        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "성공적으로 수정이 완료되었습니다", result));
    }

}

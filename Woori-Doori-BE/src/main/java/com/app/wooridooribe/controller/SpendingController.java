package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.CardHistoryResponseDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.spending.SpendingService;
import com.app.wooridooribe.controller.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "소비내역", description = "소비 내역 조회, 더치페이, 인원 수정, 금액 수정")
@RestController
@RequiredArgsConstructor
@RequestMapping("/history/calendar")
public class SpendingController {

    private final SpendingService spendingService;

    @Operation(summary = "월별 소비 내역 조회", description = "사용자의 특정 월에 해당하는 소비 내역 및 합계 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 날짜 형식")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역이 존재하지 않음")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMonthlySpending(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회 기준 날짜 (해당 월 전체 조회)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        Map<String, Object> result = spendingService.getMonthlySpendings(memberId, targetDate);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "조회에 성공하였습니다", result));
    }

    @Operation(summary = "소비 내역 상세 조회", description = "특정 소비 내역의 상세 정보를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역이 존재하지 않음")
    @GetMapping("/detail/{historyId}")
    public ResponseEntity<ApiResponse<CardHistoryResponseDto>> getSpendingDetail(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 소비 내역 ID", required = true)
            @PathVariable Long historyId
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        CardHistoryResponseDto result = spendingService.getSpendingDetail(historyId, memberId);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "상세 내역 조회에 성공하였습니다", result));
    }

    @Operation(summary = "지출 합계 포함 여부 수정", description = "특정 소비 내역을 지출 합계에 포함/제외시킵니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "수정 실패")
    @PatchMapping("/{historyId}/{includeInTotal}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateIncludeTotal(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "소비 내역 ID", required = true)
            @PathVariable Long historyId,
            @Parameter(description = "지출 합계 포함 여부", required = true)
            @PathVariable boolean includeInTotal
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        spendingService.updateIncludeTotal(historyId, memberId, includeInTotal);

        Map<String, Object> result = Map.of(
                "historyId", historyId,
                "includeInTotal", includeInTotal
        );

        String message = includeInTotal ? "지출 합계에 포함되었습니다" : "지출 합계에서 제외되었습니다";

        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), message, result));
    }

    @Operation(summary = "카테고리 수정", description = "소비 내역의 카테고리를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 카테고리 값")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "수정 실패")
    @PatchMapping("/{historyId}/category")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCategory(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "소비 내역 ID", required = true)
            @PathVariable Long historyId,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "변경할 카테고리 정보",
                    required = true
            )
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

    @Operation(summary = "더치페이 인원 수정", description = "소비 내역의 더치페이 인원 수를 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 인원 수")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "수정 실패")
    @PatchMapping("/{historyId}/dutchpay")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateDutchpay(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "소비 내역 ID", required = true)
            @PathVariable Long historyId,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "수정할 인원 수 정보",
                    required = true
            )
            @org.springframework.web.bind.annotation.RequestBody Map<String, Integer> request
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

    @Operation(summary = "소비 금액 수정", description = "소비 내역의 금액을 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 금액 값")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "소비 내역 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "수정 실패")
    @PatchMapping("/{historyId}/price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePrice(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "소비 내역 ID", required = true)
            @PathVariable Long historyId,
            @io.swagger.v3.oas.annotations.media.Schema(
                    description = "수정할 금액 정보",
                    required = true
            )
            @org.springframework.web.bind.annotation.RequestBody Map<String, Integer> request
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

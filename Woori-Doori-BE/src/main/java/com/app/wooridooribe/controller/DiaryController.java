package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.service.diary.DiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DiaryResponseDto>>> getMonthlyDiaries(
            @RequestParam Long memberId,
            @RequestParam String targetMonth
    ) {
        try {
            String[] parts = targetMonth.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);

            List<DiaryResponseDto> result = diaryService.getMonthlyDiaries(memberId, year, month);

            return ResponseEntity.ok(
                    ApiResponse.res(HttpStatus.OK.value(), "소비 일기 전체 조회 성공", result)
            );
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            throw new CustomException(ErrorCode.DIARY_INVALID_DATE);
        }
    }

    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryResponseDto>> getDiaryDetail(
            @PathVariable Long diaryId
    ) {
        DiaryResponseDto result = diaryService.getDiaryDetail(diaryId);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "소비 일기 상세 조회 성공", result));
    }
}

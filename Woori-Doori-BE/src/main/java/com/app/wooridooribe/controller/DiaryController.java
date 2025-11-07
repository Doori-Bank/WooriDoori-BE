package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.DiaryCreateRequestDto;
import com.app.wooridooribe.controller.dto.DiaryCreateResponseDto;
import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import com.app.wooridooribe.controller.dto.DiaryUpdateRequestDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.diary.DiaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "소비일기", description = "소비 일기 관련 CRUD API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/diary")
public class DiaryController {

    private final DiaryService diaryService;

    @Operation(summary = "소비 일기 전체 조회", description = "특정 월에 해당하는 소비 일기 전체를 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "일기가 존재하지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DiaryResponseDto>>> getMonthlyDiaries(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회 기준 날짜 (해당 월 전체 조회)", required = true)

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate targetDate
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        List<DiaryResponseDto> result = diaryService.getMonthlyDiaries(memberId, targetDate);

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "조회에 성공하였습니다", result)
        );
    }

    @Operation(summary = "소비 일기 상세 조회", description = "특정 소비 일기의 상세 내용을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "상세 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 일기 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @GetMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryResponseDto>> getDiaryDetail(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "조회할 일기의 고유 ID", required = true) @PathVariable Long diaryId
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        DiaryResponseDto result = diaryService.getDiaryDetail(diaryId, memberId);
        return ResponseEntity.ok(ApiResponse.res(HttpStatus.OK.value(), "상세 내역 조회에 성공하였습니다", result));
    }

    @Operation(summary = "소비 일기 입력", description = "새로운 소비 일기를 작성합니다. (1일 1개 제한)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입력 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값이 유효하지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "해당 날짜의 일기가 이미 존재")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "소비 일기 생성에 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @PostMapping("/insertDiary")
    public ResponseEntity<ApiResponse<DiaryCreateResponseDto>> createDiary(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "소비 일기 생성 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DiaryCreateRequestDto.class))
            )
            @RequestBody DiaryCreateRequestDto request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        DiaryCreateResponseDto response = diaryService.createDiary(memberId, request);
        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "소비 일기 입력을 성공하였습니다", response)
        );
    }

    @Operation(summary = "소비 일기 수정", description = "특정 소비 일기의 감정/내용을 수정합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 값이 유효하지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "수정 권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 일기 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "수정 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @PutMapping("/updateDiary/{diaryId}")
    public ResponseEntity<ApiResponse<DiaryResponseDto>> updateDiary(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "수정할 일기의 ID", required = true)
            @PathVariable Long diaryId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "소비 일기 수정 요청 바디",
                    required = true,
                    content = @Content(schema = @Schema(implementation = DiaryUpdateRequestDto.class))
            )
            @RequestBody DiaryUpdateRequestDto request
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        DiaryResponseDto result = diaryService.updateDiary(diaryId, memberId, request);
        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "일기를 성공적으로 수정했습니다", result)
        );
    }

    @Operation(summary = "소비 일기 삭제", description = "특정 소비 일기를 삭제합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "삭제 권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 일기 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "삭제 실패")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    @DeleteMapping("/{diaryId}")
    public ResponseEntity<ApiResponse<Void>> deleteDiary(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "삭제할 일기의 ID", required = true)
            @PathVariable Long diaryId
    ) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        diaryService.deleteDiary(diaryId, memberId);
        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "일기를 성공적으로 삭제했습니다")
        );
    }

}

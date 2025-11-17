package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.*;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.goal.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "목표", description = "목표 설정 및 수정 관련 API")
public class GoalController {

    private final GoalService goalService;

    /** 목표 설정 API **/

    @PutMapping("/setgoal")
    @Operation(summary = "목표 금액 설정/수정", description = "이번 달 목표가 없으면 이번 달 목표를 설정하고, 이미 있다면 다음 달 목표를 등록하거나 수정합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 설정 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "422", description = "잘못된 값을 입력하였습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "목표치는 급여보다 클 수 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    public ResponseEntity<ApiResponse<SetGoalDto>> setGoal(Authentication authentication,
            @RequestBody SetGoalDto setGoalDto) {

        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        String resultMsg;

        GoalResponseDto result = goalService.setGoal(memberId, setGoalDto);

        // resultMsg (이번 달 설정 / 다음 달 등록 / 수정됨)
        if (!result.isThisMonthGoalExists()) {
            resultMsg = "이번 달 목표를 설정했어요";
        } else if (!result.isNextMonthGoalExists()) {
            resultMsg = "다음 달 목표를 등록했어요";
        } else {
            resultMsg = "다음 달 목표를 수정했어요";
        }

        return ResponseEntity.ok(ApiResponse.res(200, resultMsg, result.getGoalData()));
    }
    
    /** 목표 달성도 점수 계산 API **/
    
    @PostMapping("/calculate-scores")
    @Operation(summary = "목표 달성도 점수 계산", 
               description = "이번 달 목표에 대한 4가지 점수(달성도, 안정성, 비율, 지속성)를 계산하여 업데이트하고, 카테고리별 소비내역과 함께 반환합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "점수 계산 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이번 달 목표가 존재하지 않습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    public ResponseEntity<ApiResponse<GoalScoreResponseDto>> calculateGoalScores(Authentication authentication) {
        
        // 현재 로그인한 사용자의 memberId 추출
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        
        log.info("목표 점수 계산 요청 - memberId: {}", memberId);
        
        // 점수 계산 및 업데이트 (카테고리별 소비내역 포함)
        GoalScoreResponseDto result = goalService.calculateAndUpdateGoalScores(memberId);
        
        return ResponseEntity.ok(ApiResponse.res(200, "목표 달성도 점수가 계산되었습니다", result));
    }
    
    /** 대시보드 화면용 API **/
    
    @GetMapping("/dashboard")
    @Operation(summary = "대시보드 데이터 조회", 
               description = "이번달 목표 금액, 달성률, 소비 점수, 두리의 한마디, 카테고리별 소비 TOP 4를 반환합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "대시보드 데이터 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이번 달 목표가 존재하지 않습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getDashboard(Authentication authentication) {
        
        // 현재 로그인한 사용자의 memberId 추출
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        
        log.info("대시보드 데이터 조회 요청 - memberId: {}", memberId);
        
        // 대시보드 데이터 조회
        DashboardResponseDto result = goalService.getDashboardData(memberId);
        
        return ResponseEntity.ok(ApiResponse.res(200, "대시보드 데이터를 불러왔습니다", result));
    }
    
    /** 과거 목표 조회 API **/
    
    @GetMapping("/past")
    @Operation(summary = "과거 목표 데이터 조회", 
               description = "특정 년/월의 목표 금액, 달성률, 소비 점수, 두리의 한마디, 카테고리별 소비 TOP 4를 반환합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "과거 목표 데이터 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "해당 월의 목표가 존재하지 않습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    public ResponseEntity<ApiResponse<DashboardResponseDto>> getPastGoal(
            Authentication authentication,
            @RequestParam int year,
            @RequestParam int month) {
        
        // 현재 로그인한 사용자의 memberId 추출
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        
        log.info("과거 목표 데이터 조회 요청 - memberId: {}, year: {}, month: {}", memberId, year, month);
        
        // 과거 목표 데이터 조회
        DashboardResponseDto result = goalService.getPastGoalData(memberId, year, month);
        
        return ResponseEntity.ok(ApiResponse.res(200, "과거 목표 데이터를 불러왔습니다", result));
    }



    /**
     * 목표 히스토리 조회 API
     **/
    @GetMapping("/getgoalhistory")
    @Operation(summary = "목표 금액 히스토리 보기", description = "로그인한 아이디로 등록한 목표금액 기록을 전부 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목표 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록이 존재하지 않음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)")
    public ResponseEntity<ApiResponse<List<GetGoalDto>>> getGoalHistory(Authentication authentication) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long userId = principal.getId();

        List<GetGoalDto> result = goalService.getGoalHistory(userId);
        return ResponseEntity.ok(ApiResponse.res(200, "목표 히스토리를 불러왔어요", result));

    }
}

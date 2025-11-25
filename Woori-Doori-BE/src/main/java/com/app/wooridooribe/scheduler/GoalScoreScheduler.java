package com.app.wooridooribe.scheduler;

import com.app.wooridooribe.service.goal.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 목표 점수 계산 스케줄러
 * 매달 마지막 날 새벽 2시에 모든 활성 유저의 점수를 계산하여 DB에 저장
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalScoreScheduler {
    
    private final GoalService goalService;
    
    /**
     * 매일 새벽 2시에 실행되지만, 실제로는 매달 마지막 날에만 처리
     * cron 표현식: 초 분 시 일 월 요일
     * 0 0 2 * * ? = 매일 02:00:00
     */
   @Scheduled(cron = "0 0 2 * * ?")
    public void calculateAllUsersScores() {
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        // 오늘이 해당 월의 마지막 날이 아니면 실행하지 않음
        if (!today.equals(lastDayOfMonth)) {
            log.debug("오늘은 해당 월의 마지막 날이 아닙니다. 스케줄러를 건너뜁니다. (오늘: {}, 마지막 날: {})", 
                    today, lastDayOfMonth);
            return;
        }
        
        log.info("=== 배치 점수 계산 스케줄러 시작 (매달 마지막 날: {}) ===", today);
        
        try {
            int processedCount = goalService.calculateAllActiveUsersScores();
            log.info("=== 배치 점수 계산 완료 - 처리된 유저 수: {} ===", processedCount);
        } catch (Exception e) {
            log.error("=== 배치 점수 계산 중 에러 발생 ===", e);
        }
    }
    
    /**
     * 테스트용: 매 5분마다 실행 (개발 환경에서만 사용)
     * 운영 환경에서는 주석 처리하거나 삭제
     */
    // @Scheduled(fixedRate = 300000) // 5분 = 300000ms
    // public void calculateAllUsersScoresTest() {
    //     log.info("=== [테스트] 배치 점수 계산 스케줄러 시작 ===");
    //     try {
    //         int processedCount = goalService.calculateAllActiveUsersScores();
    //         log.info("=== [테스트] 배치 점수 계산 완료 - 처리된 유저 수: {} ===", processedCount);
    //     } catch (Exception e) {
    //         log.error("=== [테스트] 배치 점수 계산 중 에러 발생 ===", e);
    //     }
    // }
}


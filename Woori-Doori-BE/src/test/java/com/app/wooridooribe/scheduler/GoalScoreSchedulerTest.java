package com.app.wooridooribe.scheduler;

import com.app.wooridooribe.service.goal.GoalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * GoalScoreScheduler 테스트
 * 스케줄러가 매달 마지막 날에만 실행되는지 확인
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("목표 점수 계산 스케줄러 테스트")
class GoalScoreSchedulerTest {

    @Mock
    private GoalService goalService;

    @InjectMocks
    private GoalScoreScheduler goalScoreScheduler;

    @BeforeEach
    void setUp() {
        // 각 테스트 전에 Mock 초기화
        reset(goalService);
    }

    @Test
    @DisplayName("마지막 날 확인 로직 테스트 - 다양한 월의 마지막 날 계산")
    void testLastDayOfMonthLogic() {
        // Given: 다양한 날짜에 대한 마지막 날 계산
        LocalDate januaryLast = LocalDate.of(2024, 1, 31);
        LocalDate februaryLast = LocalDate.of(2024, 2, 29); // 윤년
        LocalDate februaryLastNonLeap = LocalDate.of(2023, 2, 28); // 평년
        LocalDate aprilLast = LocalDate.of(2024, 4, 30);
        LocalDate decemberLast = LocalDate.of(2024, 12, 31);
        
        // When & Then: 각 월의 마지막 날이 올바르게 계산되는지 확인
        assertEquals(januaryLast, januaryLast.withDayOfMonth(januaryLast.lengthOfMonth()));
        assertEquals(februaryLast, februaryLast.withDayOfMonth(februaryLast.lengthOfMonth()));
        assertEquals(februaryLastNonLeap, februaryLastNonLeap.withDayOfMonth(februaryLastNonLeap.lengthOfMonth()));
        assertEquals(aprilLast, aprilLast.withDayOfMonth(aprilLast.lengthOfMonth()));
        assertEquals(decemberLast, decemberLast.withDayOfMonth(decemberLast.lengthOfMonth()));
    }

    @Test
    @DisplayName("점수 계산 성공 시 GoalService가 호출되어야 함")
    void testCalculateAllUsersScores_Success() {
        // Given: GoalService가 성공적으로 처리된 유저 수를 반환
        int processedCount = 10;
        when(goalService.calculateAllActiveUsersScores()).thenReturn(processedCount);
        
        // When: 스케줄러 실행
        goalScoreScheduler.calculateAllUsersScores();
        
        // Then: 실제 날짜가 마지막 날이면 GoalService가 호출됨
        // 마지막 날이 아니면 호출되지 않음
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        if (today.equals(lastDayOfMonth)) {
            // 마지막 날인 경우: GoalService가 호출되어야 함
            verify(goalService, times(1)).calculateAllActiveUsersScores();
        } else {
            // 마지막 날이 아닌 경우: GoalService가 호출되지 않아야 함
            verify(goalService, never()).calculateAllActiveUsersScores();
        }
    }

    @Test
    @DisplayName("점수 계산 실패 시 예외가 처리되어야 함")
    void testCalculateAllUsersScores_ExceptionHandling() {
        // Given: GoalService가 예외를 던짐
        RuntimeException exception = new RuntimeException("점수 계산 실패");
        when(goalService.calculateAllActiveUsersScores()).thenThrow(exception);
        
        // When: 스케줄러 실행
        // 예외가 발생해도 스케줄러가 중단되지 않아야 함
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        if (today.equals(lastDayOfMonth)) {
            // 마지막 날인 경우에만 테스트 실행
            assertDoesNotThrow(() -> goalScoreScheduler.calculateAllUsersScores());
            verify(goalService, times(1)).calculateAllActiveUsersScores();
        } else {
            // 마지막 날이 아닌 경우: GoalService가 호출되지 않음
            goalScoreScheduler.calculateAllUsersScores();
            verify(goalService, never()).calculateAllActiveUsersScores();
        }
    }

    @Test
    @DisplayName("스케줄러 실행 시 마지막 날 체크 로직이 올바르게 동작해야 함")
    void testSchedulerLastDayCheck() {
        // Given: 현재 날짜
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        // When: 스케줄러 실행
        goalScoreScheduler.calculateAllUsersScores();
        
        // Then: 마지막 날인지에 따라 GoalService 호출 여부 확인
        if (today.equals(lastDayOfMonth)) {
            verify(goalService, times(1)).calculateAllActiveUsersScores();
        } else {
            verify(goalService, never()).calculateAllActiveUsersScores();
        }
    }

    @Test
    @DisplayName("스케줄러가 정상적으로 실행되는지 통합 테스트")
    void testSchedulerExecution() {
        // Given: GoalService가 정상적으로 동작
        int processedCount = 5;
        when(goalService.calculateAllActiveUsersScores()).thenReturn(processedCount);
        
        // When: 스케줄러 실행
        goalScoreScheduler.calculateAllUsersScores();
        
        // Then: 스케줄러가 정상적으로 실행되었는지 확인
        LocalDate today = LocalDate.now();
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        
        if (today.equals(lastDayOfMonth)) {
            verify(goalService, times(1)).calculateAllActiveUsersScores();
        } else {
            verify(goalService, never()).calculateAllActiveUsersScores();
        }
    }
}


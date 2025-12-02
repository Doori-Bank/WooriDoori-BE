package com.app.wooridooribe.batch.processor;

import com.app.wooridooribe.batch.dto.ProcessResult;
import com.app.wooridooribe.controller.dto.GoalScoreResponseDto;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.service.goal.GoalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 각 유저의 목표 점수를 계산하는 ItemProcessor
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalScoreItemProcessor implements ItemProcessor<Member, ProcessResult> {

    private final GoalService goalService;

    @Override
    public ProcessResult process(Member member) throws Exception {
        try {
            // 점수 계산 및 업데이트
            GoalScoreResponseDto result = goalService.calculateAndUpdateGoalScoresBatch(member.getId());
            
            if (result != null) {
                log.debug("점수 계산 완료 - memberId: {}", member.getId());
                // 성공한 경우 ProcessResult 반환
                return ProcessResult.success(member);
            } else {
                log.debug("목표 없음으로 스킵 - memberId: {}", member.getId());
                // 목표가 없는 경우 스킵으로 표시
                return ProcessResult.skipped(member, "목표 없음");
            }
        } catch (Exception e) {
            log.error("점수 계산 실패 - memberId: {}", member.getId(), e);
            // 실패한 경우 실패로 표시
            return ProcessResult.failed(member, e.getMessage());
        }
    }
}


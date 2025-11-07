package com.app.wooridooribe.service.goal;

import com.app.wooridooribe.controller.dto.GoalDto;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.goal.GoalRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;

    @Override
    public GoalDto setGoal(GoalDto goalDto) {

        // ✅ 1. 입력값 검증
        if (goalDto.getGoalIncome() == null || goalDto.getGoalIncome().isEmpty()) {
            throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
        }
        if (goalDto.getPreviousGoalMoney() == null) {
            throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
        }

        // ✅ 2. Member 존재 여부 확인
        Member member = memberRepository.findById(goalDto.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // ✅ 3. 논리 검증 - 수입보다 목표소비금액이 클 경우
        try {
            int income = Integer.parseInt(goalDto.getGoalIncome());
            if (income < goalDto.getPreviousGoalMoney()) {
                throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
            }
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
        }

        // ✅ 4. Goal 엔티티 생성
        Goal goal = Goal.builder()
                .member(member)
                .goalStartDate(goalDto.getGoalStartDate() != null ? goalDto.getGoalStartDate() : LocalDate.now())
                .previousGoalMoney(goalDto.getPreviousGoalMoney())
                .goalJob(goalDto.getGoalJob())
                .goalIncome(goalDto.getGoalIncome())
                .goalScore(goalDto.getGoalScore())
                .goalComment(goalDto.getGoalComment())
                .build();

        // ✅ 5. DB 저장
        Goal savedGoal = goalRepository.save(goal);

        // ✅ 6. DTO 변환 후 반환
        return GoalDto.builder()
                .goalId(savedGoal.getId())
                .memberId(member.getId())
                .goalStartDate(savedGoal.getGoalStartDate())
                .previousGoalMoney(savedGoal.getPreviousGoalMoney())
                .goalScore(savedGoal.getGoalScore())
                .goalComment(savedGoal.getGoalComment())
                .goalJob(savedGoal.getGoalJob())
                .goalIncome(savedGoal.getGoalIncome())
                .build();
    }
}
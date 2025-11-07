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
        try {
            // 1️⃣ 입력값 검증
            if (goalDto.getGoalIncome() == null || goalDto.getGoalIncome().isEmpty()) {
                throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
            }
            if (goalDto.getPreviousGoalMoney() == null) {
                throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
            }

            // Member 엔티티 조회
            Member member = memberRepository.findById(goalDto.getMemberId())
                    .orElseThrow(() ->  new CustomException(ErrorCode.INVALID_USER));

            // 수입보다 소비금액이 큰 경우
            try {
                int income = Integer.parseInt(goalDto.getGoalIncome());
                if (income < goalDto.getPreviousGoalMoney()) {
                    throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
                }
            } catch (NumberFormatException e) {
                throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
            }

            // Goal 엔티티 생성 및 저장
            Goal goal = Goal.builder()
                    .member(member)
                    .goalStartDate(goalDto.getGoalStartDate() != null ? goalDto.getGoalStartDate() : LocalDate.now())
                    .previousGoalMoney(goalDto.getPreviousGoalMoney())
                    .goalJob(goalDto.getGoalJob())
                    .goalIncome(goalDto.getGoalIncome())
                    .goalScore(goalDto.getGoalScore())
                    .goalComment(goalDto.getGoalComment())
                    .build();

            Goal savedGoal = goalRepository.save(goal);

            // 반환용 DTO 변환
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

        } catch (IllegalArgumentException e) {
            // 서비스 내에서 의미 있는 예외를 잡아서 다시 던짐
            throw new RuntimeException("목표 설정 실패: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("서버 내부 오류로 인해 목표 설정에 실패했습니다.", e);
        }
    }
}

package com.app.wooridooribe.service.goal;

import com.app.wooridooribe.controller.dto.GoalResponseDto;
import com.app.wooridooribe.controller.dto.SetGoalDto;
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
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;

    @Override
    public GoalResponseDto setGoal(Long memberId, SetGoalDto setGoalDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (setGoalDto == null ||
                setGoalDto.getGoalJob() == null ||
                setGoalDto.getGoalIncome() == null ||
                setGoalDto.getPreviousGoalMoney() == null) {
            throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
        }

        if((Integer.parseInt(setGoalDto.getGoalIncome())<setGoalDto.getPreviousGoalMoney())
                && (!(setGoalDto.getGoalJob().equals("무직") || setGoalDto.getGoalJob().equals("학생")))) {
            //제한금액이 급여보다 클 경우
            throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
        }

        //이번 달과 다음 달 목표 조회 (QueryDSL)
        List<Goal> goals = goalRepository.findGoalsForThisAndNextMonth(member);
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = thisMonth.plusMonths(1);

        boolean thisMonthGoalExists = goals.stream()
                .anyMatch(g -> g.getGoalStartDate().equals(thisMonth));
        boolean nextMonthGoalExists = goals.stream()
                .anyMatch(g -> g.getGoalStartDate().equals(nextMonth));

        Goal goal;
        if (!thisMonthGoalExists) {
            // 이번 달 목표 등록
            goal = Goal.builder()
                    .member(member)
                    .goalStartDate(thisMonth)
                    .previousGoalMoney(setGoalDto.getPreviousGoalMoney())
                    .goalJob(setGoalDto.getGoalJob())
                    .goalIncome(setGoalDto.getGoalIncome())
                    .goalScore(0)
                    .build();
            goalRepository.save(goal);
        } else if (!nextMonthGoalExists) {
            // 다음 달 목표 등록
            goal = Goal.builder()
                    .member(member)
                    .goalStartDate(nextMonth)
                    .previousGoalMoney(setGoalDto.getPreviousGoalMoney())
                    .goalJob(setGoalDto.getGoalJob())
                    .goalIncome(setGoalDto.getGoalIncome())
                    .goalScore(0)
                    .build();
            goalRepository.save(goal);
        } else {
            // 다음 달 목표 수정
            goal = goals.stream()
                    .filter(g -> g.getGoalStartDate().equals(nextMonth))
                    .findFirst()
                    .get();
            goal.setPreviousGoalMoney(setGoalDto.getPreviousGoalMoney());
            goal.setGoalJob(setGoalDto.getGoalJob());
            goal.setGoalIncome(setGoalDto.getGoalIncome());
            goalRepository.save(goal);
        }

        return GoalResponseDto.builder()
                .thisMonthGoalExists(thisMonthGoalExists)
                .nextMonthGoalExists(nextMonthGoalExists)
                .goalData(SetGoalDto.builder()
                        .goalJob(goal.getGoalJob())
                        .goalIncome(goal.getGoalIncome())
                        .previousGoalMoney(goal.getPreviousGoalMoney())
                        .build())
                .build();

    }
}
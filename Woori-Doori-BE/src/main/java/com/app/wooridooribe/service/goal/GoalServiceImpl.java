package com.app.wooridooribe.service.goal;

import com.app.wooridooribe.controller.dto.GoalDto;
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

@Service
@RequiredArgsConstructor
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;

    @Override
    public SetGoalDto setGoal(Long memberId, SetGoalDto setGoalDto) {

        // 1️⃣ Member 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 회원 ID입니다."));

        // 2️⃣ Goal 엔티티 생성
        Goal goal = Goal.builder()
                .member(member)
                .goalStartDate(LocalDate.now())
                .previousGoalMoney(setGoalDto.getPreviousGoalMoney())
                .goalJob(setGoalDto.getGoalJob())
                .goalIncome(setGoalDto.getGoalIncome())
                .goalScore(100)
                .goalComment("굿 목표")
                .build();

        goalRepository.save(goal);   // 저장

        // 응답 DTO 생성 (memberId 없이)
        return SetGoalDto.builder()
                .goalJob(goal.getGoalJob())
                .goalIncome(goal.getGoalIncome())
                .previousGoalMoney(goal.getPreviousGoalMoney())
                .build();
    }
}
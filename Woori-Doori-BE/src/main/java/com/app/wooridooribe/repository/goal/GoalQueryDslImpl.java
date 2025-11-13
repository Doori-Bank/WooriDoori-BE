package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.QGoal;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GoalQueryDslImpl implements GoalQueryDsl {

    private final JPAQueryFactory queryFactory;
    private final MemberRepository memberRepository;

    @Override
    public Optional<Goal> findCurrentMonthGoalByMemberId(Long memberId) {
        QGoal goal = QGoal.goal;

        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);

        Goal result = queryFactory
                .selectFrom(goal)
                .where(
                        goal.member.id.eq(memberId),
                        goal.goalStartDate.eq(thisMonth)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Goal> findGoalsForThisAndNextMonth(Member member) {
        QGoal goal = QGoal.goal;
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = thisMonth.plusMonths(1);

        return queryFactory
                .selectFrom(goal)
                .where(
                        goal.member.eq(member),
                        goal.goalStartDate.in(thisMonth, nextMonth)
                )
                .fetch();
    }

    @Override
    public Optional<Goal> findGoalByMemberIdAndStartDate(Long memberId, LocalDate startDate) {
        QGoal goal = QGoal.goal;

        Goal result = queryFactory
                .selectFrom(goal)
                .where(
                        goal.member.id.eq(memberId),
                        goal.goalStartDate.eq(startDate)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<Goal> findAllGoalsByMember(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        QGoal goal = QGoal.goal;

        // QueryDSL로 해당 멤버의 Goal 조회
        return queryFactory
                .selectFrom(goal)
                .where(goal.member.eq(member))
                .orderBy(goal.goalStartDate.asc())
                .fetch();
    }
}



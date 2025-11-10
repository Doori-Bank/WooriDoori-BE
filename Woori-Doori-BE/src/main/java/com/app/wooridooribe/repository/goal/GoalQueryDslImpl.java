package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.QGoal;
import com.app.wooridooribe.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GoalQueryDslImpl implements GoalQueryDsl {

    private final JPAQueryFactory queryFactory;

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
}


package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.QGoal;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GoalQueryDslImpl implements GoalQueryDsl {

    private final JPAQueryFactory queryFactory;

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
}



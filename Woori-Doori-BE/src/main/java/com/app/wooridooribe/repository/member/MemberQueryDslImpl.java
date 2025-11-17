package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.QMember;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MemberQueryDslImpl implements MemberQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Member> findMembersLoggedInWithinThreeMonths(LocalDateTime threeMonthsAgo) {
        QMember m = QMember.member;

        return queryFactory
                .selectFrom(m)
                .where(
                        m.lastLoginDate.goe(threeMonthsAgo)
                )
                .fetch();
    }
}



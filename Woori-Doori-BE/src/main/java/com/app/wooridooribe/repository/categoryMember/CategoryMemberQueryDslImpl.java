package com.app.wooridooribe.repository.categoryMember;

import com.app.wooridooribe.entity.QCategoryMember;
import com.app.wooridooribe.entity.type.CategoryType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CategoryMemberQueryDslImpl implements CategoryMemberQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CategoryType> findEssentialCategoryNamesByMemberId(Long memberId) {
        QCategoryMember categoryMember = QCategoryMember.categoryMember;

        return queryFactory
                .select(categoryMember.categoryType)
                .from(categoryMember)
                .where(
                        categoryMember.member.id.eq(memberId)
                )
                .fetch();
    }
}


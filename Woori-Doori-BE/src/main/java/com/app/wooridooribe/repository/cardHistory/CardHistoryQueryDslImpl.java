package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.QCard;
import com.app.wooridooribe.entity.QCardHistory;
import com.app.wooridooribe.entity.QMemberCard;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.entity.type.YESNO;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CardHistoryQueryDslImpl implements CardHistoryQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public CardHistorySummaryResponseDto findByUserAndMonthAndStatus(Long userId, int year, int month, StatusType status) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        // 한 번의 쿼리로 전체 리스트 조회
        List<CardHistory> histories = queryFactory
                .selectFrom(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(userId),
                        history.historyStatus.eq(status),
                        history.historyDate.year().eq(year),
                        history.historyDate.month().eq(month)
                )
                .orderBy(history.historyDate.asc())
                .fetch();

        int totalAmount = histories.stream()
                .mapToInt(CardHistory::getHistoryPrice)
                .sum();

        return new CardHistorySummaryResponseDto(totalAmount, histories);
    }

    @Override
    public CardHistory findDetailById(Long historyId) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        return queryFactory
                .selectFrom(history)
                .join(history.memberCard, memberCard).fetchJoin()   // memberCard 같이 가져옴
                .where(history.id.eq(historyId))
                .fetchOne();
    }

    @Override
    @Transactional
    public void updateIncludeTotal(Long historyId, boolean includeTotal) {
        QCardHistory ch = QCardHistory.cardHistory;

        queryFactory
                .update(ch)
                .set(ch.historyIncludeTotal, includeTotal ? "Y" : "N")
                .where(ch.id.eq(historyId))
                .execute();
    }

    @Override
    @Transactional
    public void updateIncludeTotalByMemberAndCategories(Long memberId, List<CategoryType> categories, boolean includeTotal) {
        QCardHistory ch = QCardHistory.cardHistory;
        QMemberCard mc = QMemberCard.memberCard;

        queryFactory
                .update(ch)
                .set(ch.historyIncludeTotal, includeTotal ? "Y" : "N")
                .where(
                        ch.memberCard.isNotNull(),
                        ch.memberCard.eq(mc),
                        mc.member.id.eq(memberId),
                        ch.historyCategory.in(categories)
                )
                .execute();
    }

    @Override
    @Transactional
    public void updateCategory(Long historyId, CategoryType newCategory) {
        QCardHistory ch = QCardHistory.cardHistory;

        queryFactory
                .update(ch)
                .set(ch.historyCategory, newCategory)
                .where(ch.id.eq(historyId))
                .execute();
    }

    @Override
    @Transactional
    public void updateDutchpay(Long historyId, int count) {
        QCardHistory ch = QCardHistory.cardHistory;

        queryFactory
                .update(ch)
                .set(ch.historyDutchpay, count)
                .where(ch.id.eq(historyId))
                .execute();
    }

    @Override
    @Transactional
    public void updatePrice(Long historyId, int price) {
        QCardHistory ch = QCardHistory.cardHistory;

        queryFactory
                .update(ch)
                .set(ch.historyPrice, price)
                .where(ch.id.eq(historyId))
                .execute();
    }

    @Override
    public Integer getTotalSpentByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        Integer totalSpent = queryFactory
                .select(history.historyPrice.sum())
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y")
                )
                .fetchOne();

        log.info("getTotalSpentByMemberAndDateRange - memberId: {}, startDate: {}, endDate: {}, totalSpent: {}",
                memberId, startDate, endDate, totalSpent);

        return totalSpent;
    }

    @Override
    public List<Tuple> getCategorySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        return queryFactory
                .select(
                        history.historyCategory,
                        history.historyPrice.sum()
                )
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y")
                )
                .groupBy(history.historyCategory)
                .orderBy(history.historyPrice.sum().desc())
                .limit(5)
                .fetch();
    }

    @Override
    public List<Tuple> getAllCategorySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        return queryFactory
                .select(
                        history.historyCategory,
                        history.historyPrice.sum()
                )
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y")
                )
                .groupBy(history.historyCategory)
                .orderBy(history.historyPrice.sum().desc())
                .fetch();
    }

    @Override
    public List<Tuple> getTopUsedCards() {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;
        QCard card = QCard.card;
        return queryFactory
                .select(
                        memberCard.card.id,
                        history.id.count()
                )
                .from(history)
                .join(history.memberCard, memberCard)
                .join(memberCard.card, card)
                .where(
                        card.cardSvc.eq(YESNO.YES)
                )
                .groupBy(memberCard.card.id)
                .orderBy(history.id.count().desc())
                .limit(3)
                .fetch();
    }

    @Override
    public List<Integer> getDailySpendingByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        List<Tuple> results = queryFactory
                .select(
                        history.historyDate,
                        history.historyPrice.sum()
                )
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y")
                )
                .groupBy(history.historyDate)
                .orderBy(history.historyDate.asc())
                .fetch();

        return results.stream()
                .map(tuple -> tuple.get(1, Integer.class))
                .toList();
    }

    @Override
    public List<Tuple> getEssentialNonEssentialSpending(Long memberId, LocalDate startDate, LocalDate endDate, List<CategoryType> essentialCategories) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        // MySQL ONLY_FULL_GROUP_BY 모드 호환을 위해 두 개의 쿼리로 분리
        // 1. 필수 지출 합계
        Integer essentialSpending = queryFactory
                .select(history.historyPrice.sum().coalesce(0))
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y"),
                        history.historyCategory.in(essentialCategories)
                )
                .fetchOne();

        // 2. 비필수 지출 합계
        Integer nonEssentialSpending = queryFactory
                .select(history.historyPrice.sum().coalesce(0))
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y"),
                        history.historyCategory.notIn(essentialCategories)
                )
                .fetchOne();

        // Tuple 리스트로 변환하여 반환 (기존 인터페이스와 호환)
        List<Tuple> results = new ArrayList<>();

        // 필수 지출 Tuple 생성 (값이 0이어도 항상 반환)
        Tuple essentialTuple = queryFactory
                .select(
                        Expressions.stringTemplate("'essential'"),
                        Expressions.numberTemplate(Integer.class, "{0}", essentialSpending != null ? essentialSpending : 0)
                )
                .from(history)
                .limit(1)
                .fetchFirst();

        if (essentialTuple != null) {
            results.add(essentialTuple);
        }

        // 비필수 지출 Tuple 생성 (값이 0이어도 항상 반환)
        Tuple nonEssentialTuple = queryFactory
                .select(
                        Expressions.stringTemplate("'nonEssential'"),
                        Expressions.numberTemplate(Integer.class, "{0}", nonEssentialSpending != null ? nonEssentialSpending : 0)
                )
                .from(history)
                .limit(1)
                .fetchFirst();

        if (nonEssentialTuple != null) {
            results.add(nonEssentialTuple);
        }

        return results;
    }

}


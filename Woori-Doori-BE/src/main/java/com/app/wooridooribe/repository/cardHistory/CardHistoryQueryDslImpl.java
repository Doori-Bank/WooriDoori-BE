package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.QCardHistory;
import com.app.wooridooribe.entity.QMemberCard;
import com.app.wooridooribe.entity.type.StatusType;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
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
    public void updateCategory(Long historyId, String newCategory) {
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
                        history.historyIncludeTotal.eq(YESNO.YES)
                )
                .fetchOne();

        return totalSpent != null ? totalSpent : 0;
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
    public List<Tuple> getTopUsedCardsByMemberAndDateRange(Long memberId, LocalDate startDate, LocalDate endDate) {
        QCardHistory history = QCardHistory.cardHistory;
        QMemberCard memberCard = QMemberCard.memberCard;

        return queryFactory
                .select(
                        memberCard.card.id,
                        history.id.count()
                )
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(memberId),
                        history.historyDate.between(startDate, endDate),
                        history.historyIncludeTotal.eq("Y")
                )
                .groupBy(memberCard.card.id)
                .orderBy(history.id.count().desc())
                .limit(3)
                .fetch();
    }

}


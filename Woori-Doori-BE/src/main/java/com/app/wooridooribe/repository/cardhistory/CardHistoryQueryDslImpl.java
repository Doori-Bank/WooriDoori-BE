package com.app.wooridooribe.repository.cardhistory;

import com.app.wooridooribe.controller.dto.CardHistorySummaryResponseDto;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.QCardHistory;
import com.app.wooridooribe.entity.QMemberCard;
import com.app.wooridooribe.entity.type.StatusType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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

        // ① 거래 내역 리스트 조회
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

        // ② 합계 계산
        Integer totalAmount = queryFactory
                .select(history.historyPrice.sum())
                .from(history)
                .join(history.memberCard, memberCard)
                .where(
                        memberCard.member.id.eq(userId),
                        history.historyStatus.eq(status),
                        history.historyDate.year().eq(year),
                        history.historyDate.month().eq(month)
                )
                .fetchOne();

        // ③ DTO로 묶어서 반환
        return new CardHistorySummaryResponseDto(totalAmount != null ? totalAmount : 0, histories);
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

}


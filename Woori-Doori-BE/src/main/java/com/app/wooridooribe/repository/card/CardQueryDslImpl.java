package com.app.wooridooribe.repository.card;

import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.QCard;
import com.app.wooridooribe.entity.QCardBenefitCategory;
import com.app.wooridooribe.entity.QMemberCard;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.YESNO;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CardQueryDslImpl implements CardQueryDsl {

        private final JPAQueryFactory queryFactory;

        @Override
        public List<Card> findCardsByCategory(CategoryType categoryType) {
                QCard card = QCard.card;
                QCardBenefitCategory cardBenefitCategory = QCardBenefitCategory.cardBenefitCategory;

                List<Card> result = queryFactory
                                .select(card)
                                .distinct()
                                .from(card)
                                .join(cardBenefitCategory).on(cardBenefitCategory.card.id.eq(card.id))
                                .where(cardBenefitCategory.categoryType.eq(categoryType))
                                .limit(4)
                                .fetch();

                log.info("카드 추천 조회 - 카테고리: {}, 조회된 카드 수: {}", categoryType, result.size());
                return result;
        }

        @Override
        public List<Tuple> findPopularCardsByCategory(CategoryType categoryType, List<Long> excludeCardIds, int limit) {
                QMemberCard memberCard = QMemberCard.memberCard;
                QCard card = QCard.card;
                QCardBenefitCategory cardBenefitCategory = QCardBenefitCategory.cardBenefitCategory;

                // tbl_card_benefit_category에서 해당 카테고리 혜택이 있는 카드들을 조회하고
                // tbl_member_card에서 card_id별 누적 카운트(보유 사용자 수)를 집계하여 1~4등 추출
                BooleanBuilder whereConditions = new BooleanBuilder()
                                .and(cardBenefitCategory.categoryType.eq(categoryType))
                                .and(card.cardSvc.eq(YESNO.YES));

                // 제외할 카드 ID가 있으면 추가
                if (excludeCardIds != null && !excludeCardIds.isEmpty()) {
                        whereConditions.and(card.id.notIn(excludeCardIds));
                }

                List<Tuple> result = queryFactory
                                .select(
                                                card.id,
                                                com.querydsl.core.types.dsl.Expressions.numberTemplate(Long.class,
                                                                "COALESCE(COUNT({0}), 0)",
                                                                memberCard.id))
                                .from(cardBenefitCategory)
                                .join(cardBenefitCategory.card, card)
                                .leftJoin(memberCard).on(memberCard.card.id.eq(card.id))
                                .where(whereConditions)
                                .groupBy(card.id)
                                .orderBy(com.querydsl.core.types.dsl.Expressions
                                                .numberTemplate(Long.class, "COALESCE(COUNT({0}), 0)", memberCard.id)
                                                .desc())
                                .limit(limit)
                                .fetch();

                // 조회된 카드 ID와 누적 카운트 로그
                List<Long> resultCardIds = result.stream()
                                .map(tuple -> tuple.get(0, Long.class))
                                .collect(java.util.stream.Collectors.toList());
                log.info("인기 카드 조회 - 카테고리: {}, 제외 카드: {}, 조회된 카드 ID: {}, 카드 수: {}",
                                categoryType, excludeCardIds, resultCardIds, result.size());
                return result;
        }

        @Override
        public List<Tuple> findPopularCardsOverall(int limit) {
                QMemberCard memberCard = QMemberCard.memberCard;
                QCard card = QCard.card;

                // tbl_member_card에서 card_id별로 카운트하여 가장 많이 등록된 카드 top 4 조회
                // 서비스 중인 카드만 조회 (cardSvc = YES)
                List<Tuple> result = queryFactory
                                .select(
                                                card.id,
                                                com.querydsl.core.types.dsl.Expressions.numberTemplate(Long.class,
                                                                "COALESCE(COUNT({0}), 0)",
                                                                memberCard.id))
                                .from(card)
                                .leftJoin(memberCard).on(memberCard.card.id.eq(card.id))
                                .where(card.cardSvc.eq(YESNO.YES))
                                .groupBy(card.id)
                                .orderBy(com.querydsl.core.types.dsl.Expressions
                                                .numberTemplate(Long.class, "COALESCE(COUNT({0}), 0)", memberCard.id)
                                                .desc())
                                .limit(limit)
                                .fetch();

                // 조회된 카드 ID와 누적 카운트 로그
                List<Long> resultCardIds = result.stream()
                                .map(tuple -> tuple.get(0, Long.class))
                                .collect(java.util.stream.Collectors.toList());
                log.info("전체 인기 카드 조회 - 조회된 카드 ID: {}, 카드 수: {}", resultCardIds, result.size());
                return result;
        }
}

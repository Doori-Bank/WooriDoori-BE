package com.app.wooridooribe.repository.memberCard;

import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.entity.QCard;
import com.app.wooridooribe.entity.QFile;
import com.app.wooridooribe.entity.QMemberCard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberCardRepositoryCustomImpl implements MemberCardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    private static final QMemberCard memberCard = QMemberCard.memberCard;
    private static final QCard card = QCard.card;
    private static final QFile file = QFile.file;

    @Override
    public List<MemberCard> findByMemberIdWithCard(Long memberId) {
        return queryFactory
                .selectFrom(memberCard)
                .distinct()
                .join(memberCard.card, card).fetchJoin()
                .leftJoin(card.cardImage, file).fetchJoin()
                .where(memberCard.member.id.eq(memberId))
                .fetch();
    }

    @Override
    public Optional<MemberCard> findByMemberIdAndCardNum(Long memberId, String cardNum) {
        MemberCard result = queryFactory
                .selectFrom(memberCard)
                .join(memberCard.card, card).fetchJoin()
                .leftJoin(card.cardImage, file).fetchJoin()
                .where(
                        memberCard.member.id.eq(memberId),
                        memberCard.cardNum.eq(cardNum)
                )
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<MemberCard> findByCardNum(String cardNum) {
        MemberCard result = queryFactory
                .selectFrom(memberCard)
                .join(memberCard.card, card).fetchJoin()
                .leftJoin(card.cardImage, file).fetchJoin()
                .where(memberCard.cardNum.eq(cardNum))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}


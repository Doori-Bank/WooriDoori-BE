package com.app.wooridooribe.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCard is a Querydsl query type for Card
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCard extends EntityPathBase<Card> {

    private static final long serialVersionUID = -1423665897L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCard card = new QCard("card");

    public final StringPath annualFee1 = createString("annualFee1");

    public final StringPath annualFee2 = createString("annualFee2");

    public final QFile cardBanner;

    public final StringPath cardBenefit = createString("cardBenefit");

    public final QFile cardImage;

    public final StringPath cardName = createString("cardName");

    public final EnumPath<com.app.wooridooribe.entity.type.YESNO> cardSvc = createEnum("cardSvc", com.app.wooridooribe.entity.type.YESNO.class);

    public final EnumPath<com.app.wooridooribe.entity.type.CardType> cardType = createEnum("cardType", com.app.wooridooribe.entity.type.CardType.class);

    public final StringPath cardUrl = createString("cardUrl");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<MemberCard, QMemberCard> memberCards = this.<MemberCard, QMemberCard>createList("memberCards", MemberCard.class, QMemberCard.class, PathInits.DIRECT2);

    public QCard(String variable) {
        this(Card.class, forVariable(variable), INITS);
    }

    public QCard(Path<? extends Card> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCard(PathMetadata metadata, PathInits inits) {
        this(Card.class, metadata, inits);
    }

    public QCard(Class<? extends Card> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.cardBanner = inits.isInitialized("cardBanner") ? new QFile(forProperty("cardBanner")) : null;
        this.cardImage = inits.isInitialized("cardImage") ? new QFile(forProperty("cardImage")) : null;
    }

}


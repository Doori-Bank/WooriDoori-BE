package com.app.wooridooribe.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMemberCard is a Querydsl query type for MemberCard
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberCard extends EntityPathBase<MemberCard> {

    private static final long serialVersionUID = -1630677615L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QMemberCard memberCard = new QMemberCard("memberCard");

    public final QCard card;

    public final StringPath cardAlias = createString("cardAlias");

    public final DatePath<java.time.LocalDate> cardCreateAt = createDate("cardCreateAt", java.time.LocalDate.class);

    public final StringPath cardCvc = createString("cardCvc");

    public final ListPath<CardHistory, QCardHistory> cardHistories = this.<CardHistory, QCardHistory>createList("cardHistories", CardHistory.class, QCardHistory.class, PathInits.DIRECT2);

    public final StringPath cardNum = createString("cardNum");

    public final StringPath cardPw = createString("cardPw");

    public final StringPath cardUserName = createString("cardUserName");

    public final StringPath cardUserRegistNum = createString("cardUserRegistNum");

    public final StringPath expiryMmYy = createString("expiryMmYy");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMember member;

    public QMemberCard(String variable) {
        this(MemberCard.class, forVariable(variable), INITS);
    }

    public QMemberCard(Path<? extends MemberCard> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QMemberCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QMemberCard(PathMetadata metadata, PathInits inits) {
        this(MemberCard.class, metadata, inits);
    }

    public QMemberCard(Class<? extends MemberCard> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new QCard(forProperty("card"), inits.get("card")) : null;
        this.member = inits.isInitialized("member") ? new QMember(forProperty("member")) : null;
    }

}


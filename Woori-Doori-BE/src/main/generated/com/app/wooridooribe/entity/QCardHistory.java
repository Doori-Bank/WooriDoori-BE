package com.app.wooridooribe.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardHistory is a Querydsl query type for CardHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardHistory extends EntityPathBase<CardHistory> {

    private static final long serialVersionUID = 1127042941L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardHistory cardHistory = new QCardHistory("cardHistory");

    public final StringPath historyCategory = createString("historyCategory");

    public final DatePath<java.time.LocalDate> historyDate = createDate("historyDate", java.time.LocalDate.class);

    public final NumberPath<Integer> historyDutchpay = createNumber("historyDutchpay", Integer.class);

    public final StringPath historyIncludeTotal = createString("historyIncludeTotal");

    public final StringPath historyName = createString("historyName");

    public final NumberPath<Integer> historyPrice = createNumber("historyPrice", Integer.class);

    public final StringPath historyStatus = createString("historyStatus");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QMemberCard memberCard;

    public QCardHistory(String variable) {
        this(CardHistory.class, forVariable(variable), INITS);
    }

    public QCardHistory(Path<? extends CardHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardHistory(PathMetadata metadata, PathInits inits) {
        this(CardHistory.class, metadata, inits);
    }

    public QCardHistory(Class<? extends CardHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.memberCard = inits.isInitialized("memberCard") ? new QMemberCard(forProperty("memberCard"), inits.get("memberCard")) : null;
    }

}


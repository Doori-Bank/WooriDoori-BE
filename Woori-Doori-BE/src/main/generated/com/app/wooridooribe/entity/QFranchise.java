package com.app.wooridooribe.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFranchise is a Querydsl query type for Franchise
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFranchise extends EntityPathBase<Franchise> {

    private static final long serialVersionUID = 1255917046L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFranchise franchise = new QFranchise("franchise");

    public final QCategory category;

    public final QFile file;

    public final StringPath franName = createString("franName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public QFranchise(String variable) {
        this(Franchise.class, forVariable(variable), INITS);
    }

    public QFranchise(Path<? extends Franchise> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFranchise(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFranchise(PathMetadata metadata, PathInits inits) {
        this(Franchise.class, metadata, inits);
    }

    public QFranchise(Class<? extends Franchise> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.category = inits.isInitialized("category") ? new QCategory(forProperty("category")) : null;
        this.file = inits.isInitialized("file") ? new QFile(forProperty("file")) : null;
    }

}


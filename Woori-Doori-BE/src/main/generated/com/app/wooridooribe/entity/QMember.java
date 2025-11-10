package com.app.wooridooribe.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QMember is a Querydsl query type for Member
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMember extends EntityPathBase<Member> {

    private static final long serialVersionUID = -2053488927L;

    public static final QMember member = new QMember("member1");

    public final EnumPath<com.app.wooridooribe.entity.type.Authority> authority = createEnum("authority", com.app.wooridooribe.entity.type.Authority.class);

    public final StringPath birthBack = createString("birthBack");

    public final StringPath birthDate = createString("birthDate");

    public final ListPath<Comment, QComment> comments = this.<Comment, QComment>createList("comments", Comment.class, QComment.class, PathInits.DIRECT2);

    public final ListPath<Diary, QDiary> diaries = this.<Diary, QDiary>createList("diaries", Diary.class, QDiary.class, PathInits.DIRECT2);

    public final ListPath<Goal, QGoal> goals = this.<Goal, QGoal>createList("goals", Goal.class, QGoal.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<MemberCard, QMemberCard> memberCards = this.<MemberCard, QMemberCard>createList("memberCards", MemberCard.class, QMemberCard.class, PathInits.DIRECT2);

    public final StringPath memberId = createString("memberId");

    public final StringPath memberName = createString("memberName");

    public final ListPath<Notification, QNotification> notifications = this.<Notification, QNotification>createList("notifications", Notification.class, QNotification.class, PathInits.DIRECT2);

    public final StringPath password = createString("password");

    public final StringPath phone = createString("phone");

    public final EnumPath<com.app.wooridooribe.entity.type.StatusType> status = createEnum("status", com.app.wooridooribe.entity.type.StatusType.class);

    public QMember(String variable) {
        super(Member.class, forVariable(variable));
    }

    public QMember(Path<? extends Member> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMember(PathMetadata metadata) {
        super(Member.class, metadata);
    }

}


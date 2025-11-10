package com.app.wooridooribe.repository.diary;

import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.QDiary;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DiaryQueryDslImpl implements DiaryQueryDsl {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Diary> findByMemberAndMonth(Member member, LocalDate startDate, LocalDate endDate) {
        QDiary d = QDiary.diary;

        return queryFactory
                .selectFrom(d)
                .where(
                        d.member.eq(member),
                        d.diaryDay.between(startDate, endDate)
                )
                .orderBy(d.diaryDay.asc())
                .fetch();
    }

    @Override
    public Optional<Diary> findByMemberAndDiaryDay(Member member, LocalDate diaryDay) {
        QDiary d = QDiary.diary;

        Diary diary = queryFactory
                .selectFrom(d)
                .where(
                        d.member.eq(member),
                        d.diaryDay.eq(diaryDay)
                )
                .fetchOne();

        return Optional.ofNullable(diary);
    }
}

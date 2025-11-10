package com.app.wooridooribe.repository.diary;

import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.Member;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryQueryDsl {
    List<Diary> findByMemberAndMonth(Member member, LocalDate startDate, LocalDate endDate);

    Optional<Diary> findByMemberAndDiaryDay(Member member, LocalDate diaryDay);
}
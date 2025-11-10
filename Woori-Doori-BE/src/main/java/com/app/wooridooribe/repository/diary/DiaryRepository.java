package com.app.wooridooribe.repository.diary;

import com.app.wooridooribe.entity.Diary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaryRepository extends JpaRepository<Diary, Long>, DiaryQueryDsl {
}

package com.app.wooridooribe.service.diary;

import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import java.util.List;

public interface DiaryService {
    List<DiaryResponseDto> getMonthlyDiaries(Long memberId, int year, int month);

    DiaryResponseDto getDiaryDetail(Long diaryId);
}

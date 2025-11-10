package com.app.wooridooribe.service.diary;

import com.app.wooridooribe.controller.dto.DiaryCreateRequestDto;
import com.app.wooridooribe.controller.dto.DiaryCreateResponseDto;
import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import com.app.wooridooribe.controller.dto.DiaryUpdateRequestDto;
import java.time.LocalDate;
import java.util.List;

public interface DiaryService {
    List<DiaryResponseDto> getMonthlyDiaries(Long memberId, LocalDate targetDate);

    DiaryResponseDto getDiaryDetail(Long diaryId, Long memberId);

    DiaryCreateResponseDto createDiary(Long memberId, DiaryCreateRequestDto request);

    DiaryResponseDto updateDiary(Long diaryId, Long memberId, DiaryUpdateRequestDto request);

    void deleteDiary(Long diaryId, Long memberId);
}

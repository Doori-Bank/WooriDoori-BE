package com.app.wooridooribe.service.diary;

import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.diary.DiaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;

    @Override
    public List<DiaryResponseDto> getMonthlyDiaries(Long memberId, int year, int month) {
        validateDate(year, month);

        List<Diary> diaries = diaryRepository.findByMemberAndMonth(memberId, year, month);

        if (diaries.isEmpty()) {
            throw new CustomException(ErrorCode.DIARY_ISNULL);
        }

        return diaries.stream()
                .map(DiaryResponseDto::from)
                .toList();
    }

    @Override
    public DiaryResponseDto getDiaryDetail(Long diaryId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));

        return DiaryResponseDto.from(diary);
    }

    private void validateDate(int year, int month) {
        if (year < 2000 || month < 1 || month > 12) {
            throw new CustomException(ErrorCode.DIARY_INVALID_DATE);
        }
    }
}

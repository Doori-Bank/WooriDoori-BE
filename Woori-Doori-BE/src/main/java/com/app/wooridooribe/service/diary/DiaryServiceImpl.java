package com.app.wooridooribe.service.diary;

import com.app.wooridooribe.controller.dto.DiaryCreateRequestDto;
import com.app.wooridooribe.controller.dto.DiaryCreateResponseDto;
import com.app.wooridooribe.controller.dto.DiaryResponseDto;
import com.app.wooridooribe.controller.dto.DiaryUpdateRequestDto;
import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.type.EmotionType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.diary.DiaryRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;
    private final MemberRepository memberRepository;

    @Override
    public List<DiaryResponseDto> getMonthlyDiaries(Long memberId, LocalDate targetDate) {
        validateDate(targetDate);

        YearMonth yearMonth = YearMonth.from(targetDate);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        Member memberRef = memberRepository.getReferenceById(memberId);
        List<Diary> diaries = diaryRepository.findByMemberAndMonth(memberRef, startDate, endDate);
        
        // 일기가 없는 경우 빈 리스트 반환 (정상 상태)
        if (diaries.isEmpty()) {
            return List.of();
        }

        // 방어적 소유권 검증(쿼리로 필터되지만 레이어드 방어)
        boolean anyNotMine = diaries.stream()
                .anyMatch(d -> !memberId.equals(d.getMember().getId()));
        if (anyNotMine) {
            throw new CustomException(ErrorCode.DIARY_ISNOTYOURS);
        }

        return diaries.stream().map(DiaryResponseDto::from).toList();
    }

    @Override
    public DiaryResponseDto getDiaryDetail(Long diaryId, Long memberId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));

        assertOwnership(diary, memberId);

        return DiaryResponseDto.from(diary);
    }

    private void validateDate(LocalDate targetDate) {
        if (targetDate == null) {
            throw new CustomException(ErrorCode.DIARY_INVALID_DATE);
        }

        int year = targetDate.getYear();
        if (year < 2000 || year > LocalDate.now().getYear() + 1) {
            throw new CustomException(ErrorCode.DIARY_INVALID_DATE);
        }
    }

    @Override
    @Transactional // ⬅️ 클래스 readOnly=true 오버라이드 (쓰기 필요)
    public DiaryCreateResponseDto createDiary(Long memberId, DiaryCreateRequestDto request) {
        validateCreateRequest(request);

        Member memberRef = memberRepository.getReferenceById(memberId);

        // 하루 1개 정책: 중복 방지
        diaryRepository.findByMemberAndDiaryDay(memberRef, request.getDiaryDay())
                .ifPresent(d -> { throw new CustomException(ErrorCode.DIARY_DUPLICATE_DATE); });

        try {
            Diary saved = diaryRepository.save(
                    Diary.builder()
                            .member(memberRef)
                            .diaryDay(request.getDiaryDay())
                            .diaryEmotion(request.getDiaryEmotion())
                            .diaryContent(request.getDiaryContent())
                            .build()
            );
            return new DiaryCreateResponseDto(saved.getId(), "소비 일기가 성공적으로 등록되었습니다.");
        } catch (DataIntegrityViolationException e) {
            // DB 유니크 인덱스(uq_member_day) 하드가드 (동시성 대비)
            throw new CustomException(ErrorCode.DIARY_DUPLICATE_DATE);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DIARY_CREATE_FAILED);
        }
    }

    private void validateCreateRequest(DiaryCreateRequestDto request) {
        if (request == null || request.getDiaryDay() == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }
        EmotionType emotion = request.getDiaryEmotion();
        String content = request.getDiaryContent();
        if (emotion == null || content == null || content.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        validateDate(request.getDiaryDay());
    }

    @Override
    @Transactional
    public DiaryResponseDto updateDiary(Long diaryId, Long memberId, DiaryUpdateRequestDto request) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));

        if (!diary.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.DIARY_ISNOTYOURS);
        }

        if (request == null ||
                ((request.getDiaryEmotion() == null) &&
                        (request.getDiaryContent() == null || request.getDiaryContent().isBlank()))) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        try {
            if (request.getDiaryEmotion() != null) {
                diary.setDiaryEmotion(request.getDiaryEmotion());
            }
            if (request.getDiaryContent() != null && !request.getDiaryContent().isBlank()) {
                diary.setDiaryContent(request.getDiaryContent());
            }
            // @Transactional 이므로 flush 시 업데이트 반영
            return DiaryResponseDto.from(diary);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DIARY_UPDATE_FAIL);
        }
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId, Long memberId) {
        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> new CustomException(ErrorCode.DIARY_ISNULL));

        if (!diary.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.DIARY_ISNOTYOURS);
        }

        try {
            diaryRepository.delete(diary);
        } catch (Exception e) {
            throw new CustomException(ErrorCode.DIARY_DELETE_FAIL);
        }
    }

    // 공통 소유권 검증 헬퍼
    private void assertOwnership(Diary diary, Long memberId) {
        if (diary == null || diary.getMember() == null || diary.getMember().getId() == null) {
            throw new CustomException(ErrorCode.DIARY_ISNOTYOURS);
        }
        if (!memberId.equals(diary.getMember().getId())) {
            throw new CustomException(ErrorCode.DIARY_ISNOTYOURS);
        }
    }
}

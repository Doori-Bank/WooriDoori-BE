package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.type.EmotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "소비 일기 상세 응답 DTO")
public class DiaryResponseDto {

    @Schema(description = "소비 일기 ID", example = "10")
    private Long diaryId;

    @Schema(description = "소비 일기 날짜", example = "2025-11-07")
    private LocalDate diaryDay;

    @Schema(description = "소비 감정", example = "HAPPY")
    private EmotionType diaryEmotion;

    @Schema(description = "소비 일기 내용", example = "오늘은 점심을 예산 내로 잘 맞췄어요 😊")
    private String diaryContent;

    public static DiaryResponseDto from(Diary diary) {
        return DiaryResponseDto.builder()
                .diaryId(diary.getId())
                .diaryDay(diary.getDiaryDay())
                .diaryEmotion(diary.getDiaryEmotion())
                .diaryContent(diary.getDiaryContent())
                .build();
    }
}

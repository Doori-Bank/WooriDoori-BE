package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Diary;
import com.app.wooridooribe.entity.type.EmotionType;
import lombok.*;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DiaryResponseDto {

    private Long diaryId;
    private LocalDate diaryDay;
    private EmotionType diaryEmotion;
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

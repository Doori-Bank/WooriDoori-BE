package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.EmotionType;
import lombok.Getter;
import java.time.LocalDate;

@Getter
public class DiaryCreateRequestDto {
    private LocalDate diaryDay;
    private EmotionType diaryEmotion;
    private String diaryContent;
}

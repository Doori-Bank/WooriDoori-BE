package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.EmotionType;
import lombok.Getter;

@Getter
public class DiaryUpdateRequestDto {
    private EmotionType diaryEmotion;
    private String diaryContent;
}

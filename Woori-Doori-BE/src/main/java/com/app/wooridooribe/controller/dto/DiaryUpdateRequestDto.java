package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.EmotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소비 일기 수정 요청 DTO")
public class DiaryUpdateRequestDto {
    @Schema(description = "수정할 소비 감정 (Enum)", example = "VERY_HAPPY")
    private EmotionType diaryEmotion;

    @Schema(description = "수정할 소비 일기 내용", example = "오늘은 친구랑 커피 마셨는데 예산 초과했어요 ☕️")
    private String diaryContent;
}

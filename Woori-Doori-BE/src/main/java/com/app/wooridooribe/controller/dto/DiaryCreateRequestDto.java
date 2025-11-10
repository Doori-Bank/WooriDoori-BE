package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.EmotionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import java.time.LocalDate;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소비 일기 생성 요청 DTO")
public class DiaryCreateRequestDto {
    @Schema(description = "일기 날짜", example = "2025-11-07", required = true)
    private LocalDate diaryDay;

    @Schema(description = "소비 감정 (Enum)", example = "HAPPY", required = true)
    private EmotionType diaryEmotion;

    @Schema(description = "소비 일기 내용", example = "오늘은 점심을 예산 내로 잘 맞췄어요 😊", required = true)
    private String diaryContent;
}

package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "소비 일기 생성 응답 DTO")
public class DiaryCreateResponseDto {
    @Schema(description = "생성된 소비 일기의 ID", example = "10")
    private Long diaryId;

    @Schema(description = "결과 메시지", example = "소비 일기가 성공적으로 등록되었습니다.")
    private String message;
}

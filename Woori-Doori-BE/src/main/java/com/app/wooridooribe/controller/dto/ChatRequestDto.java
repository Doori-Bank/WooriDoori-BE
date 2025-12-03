package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "채팅 요청 DTO")
public class ChatRequestDto {

    @NotBlank(message = "메시지는 필수입니다")
    @Schema(description = "사용자 메시지", example = "안녕하세요!")
    private String message;

    @Schema(description = "조회할 연도 (선택사항, 없으면 현재 달 기준)", example = "2025")
    private Integer year;

    @Schema(description = "조회할 월 (선택사항, 없으면 현재 달 기준)", example = "12")
    private Integer month;
}

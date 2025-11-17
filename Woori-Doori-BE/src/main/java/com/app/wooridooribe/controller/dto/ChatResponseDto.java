package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "채팅 응답 DTO")
public class ChatResponseDto {

    @Schema(description = "AI 응답 메시지", example = "안녕하세요! 무엇을 도와드릴까요?")
    private String response;
}

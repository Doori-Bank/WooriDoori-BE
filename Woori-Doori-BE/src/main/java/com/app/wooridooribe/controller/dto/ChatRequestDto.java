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
}

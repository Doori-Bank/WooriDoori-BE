package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카드 별명 수정 요청 DTO")
public class CardEditRequestDto {
    @Schema(description = "사용자 카드 ID (PK)", example = "1", required = true)
    private Long id;

    @Schema(description = "카드 별명", example = "쇼핑용 카드", required = true)
    private String cardAlias;
}

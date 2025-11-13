package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "사용자 카드 등록 요청 DTO")
public class CardCreateRequestDto {
    @Schema(description = "카드 번호", example = "1234567890123456", required = true)
    private String cardNum;

    @Schema(description = "카드 비밀번호", example = "12", required = true)
    private String cardPw;

    @Schema(description = "카드 유효기간 (MMYY)", example = "1129", required = true)
    private String expiryMmYy;

    @Schema(description = "주민등록번호 앞자리", example = "990101", required = true)
    private String cardUserRegistNum;

    @Schema(description = "주민등록번호 뒷자리", example = "1", required = true)
    private String cardUserRegistBack;

    @Schema(description = "카드 CVC", example = "123", required = true)
    private String cardCvc;

    @Schema(description = "카드 별명 (선택)", example = "쇼핑용 카드")
    private String cardAlias;
}

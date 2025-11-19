package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.CardType;
import com.app.wooridooribe.entity.type.YESNO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 카드 생성 요청 DTO")
public class AdminCardCreateRequestDto {

    @NotBlank
    @Schema(description = "카드 이름", example = "WOORI SIGNATURE")
    private String cardName;

    @NotBlank
    @Schema(description = "연회비(국내)", example = "10,000원")
    private String annualFee1;

    @Schema(description = "연회비(해외)", example = "12,000원")
    private String annualFee2;

    @Schema(description = "카드 혜택 설명", example = "온라인 쇼핑 10% 청구할인")
    private String cardBenefit;

    @NotNull
    @Schema(description = "카드 타입", example = "CREDIT")
    private CardType cardType;

    @NotNull
    @Schema(description = "서비스 여부", example = "YES")
    private YESNO cardSvc;

    // 서버에서 자동 설정 (사용자 입력 불필요)
    @Schema(description = "카드 이미지 파일 ID (서버에서 자동 설정)", example = "1234", hidden = true)
    private Long cardImageFileId;

    // 서버에서 자동 설정 (사용자 입력 불필요)
    @Schema(description = "카드 배너 이미지 파일 ID (서버에서 자동 설정)", example = "5678", hidden = true)
    private Long cardBannerFileId;
}

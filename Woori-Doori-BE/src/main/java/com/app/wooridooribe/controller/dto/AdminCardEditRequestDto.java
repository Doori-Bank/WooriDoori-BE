package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.type.CardType;
import com.app.wooridooribe.entity.type.YESNO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자 카드 수정 요청 DTO")
public class AdminCardEditRequestDto {

    @NotNull
    @Schema(description = "카드 ID", example = "1", required = true)
    private Long cardId;

    @Schema(description = "카드 이름", example = "WOORI SIGNATURE")
    private String cardName;

    @Schema(description = "연회비(국내)", example = "10,000원")
    private String annualFee1;

    @Schema(description = "연회비(해외)", example = "12,000원")
    private String annualFee2;

    @Schema(description = "카드 혜택 설명", example = "온라인 쇼핑 10% 청구할인")
    private String cardBenefit;

    @Schema(description = "카드 타입", example = "CREDIT")
    private CardType cardType;

    @Schema(description = "서비스 여부", example = "YES")
    private YESNO cardSvc;

    @Schema(description = "카드 이미지 파일 ID", example = "1234")
    private Long cardImageFileId;

    @Schema(description = "카드 배너 이미지 파일 ID", example = "5678")
    private Long cardBannerFileId;
}


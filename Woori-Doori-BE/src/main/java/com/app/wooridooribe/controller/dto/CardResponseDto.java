package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.entity.type.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "카드 목록 응답 DTO")
public class CardResponseDto {
    @Schema(description = "카드 ID", example = "1")
    private Long id;

    @Schema(description = "카드명", example = "우리카드 7CORE")
    private String cardName;

    @Schema(description = "카드 이미지 URL", example = "https://cloud5-img-storage.s3.ap-northeast-2.amazonaws.com/franchise_logo/haruensoku.png")
    private String cardUrl;

    @Schema(description = "카드 혜택", example = "온라인쇼핑, 대형마트, 배달앱 10% 청구할인")
    private String cardBenef;

    @Schema(description = "카드 타입", example = "CREDIT")
    private CardType cardType;

    @Schema(description = "카드 서비스", example = "YES")
    private String cardSvc;

    @Schema(description = "연회비 1", example = "국내전용 12,000")
    private String annualFee1;

    @Schema(description = "연회비 2", example = "해외겸용 12,000")
    private String annualFee2;

    @Schema(description = "카드 이미지 File ID", example = "123")
    private Long cardImageFileId;

    @Schema(description = "카드 배너 이미지 File ID", example = "456")
    private Long cardBannerFileId;

    public static CardResponseDto toDTO(MemberCard memberCard) {
        Card card = memberCard.getCard();

        // YESNO enum을 문자열로 변환 (enum.name() 사용)
        String cardSvcStr = card.getCardSvc() != null ? card.getCardSvc().name() : null;

        // File 엔티티에서 카드 이미지 URL - file_path에 이미 전체 URL이 있으므로 그대로 사용
        String cardUrl = "";
        Long cardImageFileId = null;
        if (card.getCardImage() != null) {
            if (card.getCardImage().getFilePath() != null) {
                cardUrl = card.getCardImage().getFilePath();
            }
            cardImageFileId = card.getCardImage().getId();
        }

        // 카드 배너 이미지 정보
        Long cardBannerFileId = null;
        if (card.getCardBanner() != null) {
            cardBannerFileId = card.getCardBanner().getId();
        }

        return CardResponseDto.builder()
                .id(card.getId())
                .cardName(card.getCardName())
                .cardUrl(cardUrl)
                .cardBenef(card.getCardBenefit() != null ? card.getCardBenefit() : "")
                .cardType(card.getCardType())
                .cardSvc(cardSvcStr)
                .annualFee1(card.getAnnualFee1() != null ? card.getAnnualFee1() : "")
                .annualFee2(card.getAnnualFee2() != null ? card.getAnnualFee2() : "")
                .cardImageFileId(cardImageFileId)
                .cardBannerFileId(cardBannerFileId)
                .build();
    }

    public static CardResponseDto toDTO(Card card) {
        // YESNO enum을 문자열로 변환 (enum.name() 사용)
        String cardSvcStr = card.getCardSvc() != null ? card.getCardSvc().name() : null;

        // File 엔티티에서 카드 이미지 URL - file_path에 이미 전체 URL이 있으므로 그대로 사용
        String cardUrl = "";
        Long cardImageFileId = null;
        if (card.getCardImage() != null) {
            if (card.getCardImage().getFilePath() != null) {
                cardUrl = card.getCardImage().getFilePath();
            }
            cardImageFileId = card.getCardImage().getId();
        }

        // 카드 배너 이미지 정보
        Long cardBannerFileId = null;
        if (card.getCardBanner() != null) {
            cardBannerFileId = card.getCardBanner().getId();
        }

        return CardResponseDto.builder()
                .id(card.getId())
                .cardName(card.getCardName())
                .cardUrl(cardUrl)
                .cardBenef(card.getCardBenefit() != null ? card.getCardBenefit() : "")
                .cardType(card.getCardType())
                .cardSvc(cardSvcStr)
                .annualFee1(card.getAnnualFee1() != null ? card.getAnnualFee1() : "")
                .annualFee2(card.getAnnualFee2() != null ? card.getAnnualFee2() : "")
                .cardImageFileId(cardImageFileId)
                .cardBannerFileId(cardBannerFileId)
                .build();
    }
}

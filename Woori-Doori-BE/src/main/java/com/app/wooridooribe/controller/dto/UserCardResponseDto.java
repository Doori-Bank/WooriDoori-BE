package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.entity.type.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 카드 등록 응답 DTO")
public class UserCardResponseDto {
    @Schema(description = "사용자 카드 ID", example = "1000001")
    private Long userCardId;

    @Schema(description = "카드명", example = "우리카드 7CORE")
    private String cardName;

    @Schema(description = "카드 번호 (마스킹)", example = "4312 **** **** 1234")
    private String cardNum;

    @Schema(description = "카드 이미지 URL", example = "https://cloud5-img-storage.s3.ap-northeast-2.amazonaws.com/franchise_logo/chaghangogi.png")
    private String cardUrl;

    @Schema(description = "카드 혜택", example = "온라인쇼핑, 대형마트, 배달앱 10% 청구할인")
    private String cardBenefit;

    @Schema(description = "카드 타입", example = "CREDIT")
    private CardType cardType;

    @Schema(description = "카드 별명", example = "쇼핑용 카드")
    private String cardAlias;

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    /**
     * 카드번호 마스킹 처리 (중앙 8자리, 4자리씩 공백 구분)
     * 예: 4312123412341234 -> 4312 **** **** 1234
     */
    private static String maskCardNumber(String cardNum) {
        if (cardNum == null || cardNum.length() < 8) {
            return cardNum;
        }

        int length = cardNum.length();
        if (length == 16) {
            // 16자리 카드번호: 앞 4자리 + 공백 + **** + 공백 + **** + 공백 + 뒤 4자리
            return cardNum.substring(0, 4) + " **** **** " + cardNum.substring(12);
        } else {
            // 다른 길이: 앞 4자리 + 공백 + 마스킹 + 공백 + 나머지
            int startMask = 4;
            int endMask = Math.max(8, length - 4);
            String maskedPart = "****";
            if (length > 8) {
                maskedPart = "**** ****";
            }
            return cardNum.substring(0, startMask) + " " + maskedPart + " " + cardNum.substring(endMask);
        }
    }

    public static UserCardResponseDto from(MemberCard memberCard) {
        Card card = memberCard.getCard();

        // File 엔티티에서 카드 이미지 URL - file_path에 이미 전체 URL이 있으므로 그대로 사용
        String cardUrl = "";
        if (card.getCardImage() != null && card.getCardImage().getFilePath() != null) {
            cardUrl = card.getCardImage().getFilePath();
        }

        // member_id 추출 (null 체크)
        Long memberId = null;
        if (memberCard.getMember() != null) {
            memberId = memberCard.getMember().getId();
        }

        return UserCardResponseDto.builder()
                .userCardId(memberCard.getId())
                .cardName(card.getCardName())
                .cardNum(maskCardNumber(memberCard.getCardNum()))
                .cardUrl(cardUrl)
                .cardBenefit(card.getCardBenefit() != null ? card.getCardBenefit() : "")
                .cardType(card.getCardType())
                .cardAlias(memberCard.getCardAlias())
                .memberId(memberId)
                .build();
    }
}

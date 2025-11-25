package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.MemberCard;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "테스트용 카드 원본 데이터 DTO")
public class TestCardInfoDto {

    @Schema(description = "카드 사용자 이름")
    private final String cardUserName;

    @Schema(description = "주민등록번호 앞자리 (6자리)")
    private final String cardUserRegistNum;

    @Schema(description = "주민등록번호 뒷자리 (1자리 또는 7자리)")
    private final String cardUserRegistBack;

    @Schema(description = "카드 번호 (DB 원본 값)")
    private final String cardNum;

    @Schema(description = "카드 비밀번호 (2자리)")
    private final String cardPw;

    @Schema(description = "카드 유효기간 (MMYY)")
    private final String expiryMmYy;

    @Schema(description = "카드 CVC (3자리)")
    private final String cardCvc;

    public static TestCardInfoDto from(MemberCard memberCard) {
        return TestCardInfoDto.builder()
                .cardUserName(memberCard.getCardUserName())
                .cardUserRegistNum(memberCard.getCardUserRegistNum())
                .cardUserRegistBack(memberCard.getCardUserRegistBack())
                .cardNum(memberCard.getCardNum())
                .cardPw(memberCard.getCardPw())
                .expiryMmYy(memberCard.getExpiryMmYy())
                .cardCvc(memberCard.getCardCvc())
                .build();
    }
}


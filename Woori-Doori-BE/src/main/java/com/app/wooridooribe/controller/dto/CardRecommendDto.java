package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "카드 추천 정보")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardRecommendDto {
    @Schema(description = "카드 ID", example = "1234")
    private Long cardId;
    
    @Schema(description = "카드 배너 URL", example = "s3://mysimpleStorage")
    private String cardBannerUrl;
}


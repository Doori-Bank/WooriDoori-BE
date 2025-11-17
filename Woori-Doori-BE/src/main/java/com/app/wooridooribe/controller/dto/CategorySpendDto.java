package com.app.wooridooribe.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "카테고리별 지출 정보")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorySpendDto {
    @Schema(description = "순위", example = "top1")
    private String rank;
    
    @Schema(description = "카테고리명", example = "식비")
    private String category;
    
    @Schema(description = "총 지출 금액", example = "2000000")
    private Integer totalPrice;
}


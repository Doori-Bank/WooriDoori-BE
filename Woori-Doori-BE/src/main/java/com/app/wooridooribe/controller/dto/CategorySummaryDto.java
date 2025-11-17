package com.app.wooridooribe.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummaryDto {
    private String category; // 카테고리명
    private Long totalAmount; // 카테고리별 총 금액
    private Long count; // 거래 건수
}

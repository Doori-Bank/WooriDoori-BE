package com.app.wooridooribe.controller.dto;

import com.app.wooridooribe.entity.CardHistory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CardHistorySummaryResponseDto {
    private int totalAmount;
    private List<CardHistory> histories;
}

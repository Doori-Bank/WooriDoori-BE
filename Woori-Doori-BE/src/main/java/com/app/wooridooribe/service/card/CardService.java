package com.app.wooridooribe.service.card;

import com.app.wooridooribe.controller.dto.AdminCardCreateRequestDto;
import com.app.wooridooribe.controller.dto.AdminCardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardCreateRequestDto;
import com.app.wooridooribe.controller.dto.CardDeleteRequestDto;
import com.app.wooridooribe.controller.dto.CardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardRecommendResponseDto;
import com.app.wooridooribe.controller.dto.CardResponseDto;
import com.app.wooridooribe.controller.dto.UserCardResponseDto;
import com.app.wooridooribe.entity.File;

import java.util.List;

public interface CardService {
    List<CardResponseDto> getCardList(Long memberId);

    List<CardResponseDto> getAllCards();

    CardResponseDto createCardForAdmin(AdminCardCreateRequestDto request);

    CardResponseDto editCardForAdmin(AdminCardEditRequestDto request, File cardImageFile, File cardBannerFile);

    void deleteCardForAdmin(Long cardId);

    UserCardResponseDto createUserCard(Long memberId, CardCreateRequestDto request);

    UserCardResponseDto createUserCardWithoutCvc(Long memberId, CardCreateRequestDto request);

    void deleteCard(Long memberId, CardDeleteRequestDto request);

    void editCardAlias(Long memberId, CardEditRequestDto request);

    CardRecommendResponseDto recommendCards(Long memberId);
}

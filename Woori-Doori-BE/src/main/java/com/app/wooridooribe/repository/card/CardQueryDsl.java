package com.app.wooridooribe.repository.card;

import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.type.CategoryType;

import java.util.List;

public interface CardQueryDsl {
    /**
     * 특정 카테고리에 대한 카드 목록을 조회합니다 (최대 4개)
     * 
     * @param categoryType 카테고리 타입
     * @return 카드 목록 (최대 4개)
     */
    List<Card> findCardsByCategory(CategoryType categoryType);

    /**
     * 특정 카테고리 혜택이 있는 카드들 중 전체 사용자의 사용 횟수로 정렬하여 인기 카드를 조회합니다
     * 
     * @param categoryType   카테고리 타입
     * @param excludeCardIds 제외할 카드 ID 목록 (중복 방지)
     * @param limit          조회할 최대 개수
     * @return 카드 ID와 전체 사용자 사용 횟수 Tuple 리스트
     */
    List<com.querydsl.core.Tuple> findPopularCardsByCategory(CategoryType categoryType, List<Long> excludeCardIds,
            int limit);
}

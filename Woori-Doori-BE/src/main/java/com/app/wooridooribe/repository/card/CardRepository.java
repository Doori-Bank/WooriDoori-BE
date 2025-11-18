package com.app.wooridooribe.repository.card;

import com.app.wooridooribe.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long>, CardQueryDsl {

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.cardImage")
    List<Card> findAllWithImage();

    /**
     * 카드 ID 리스트로 카드 정보를 조회합니다.
     */
    @Query("SELECT c FROM Card c " +
            "LEFT JOIN FETCH c.cardBanner " +
            "WHERE c.id IN :cardIds")
    List<Card> findCardsByIdIn(@Param("cardIds") List<Long> cardIds);
}

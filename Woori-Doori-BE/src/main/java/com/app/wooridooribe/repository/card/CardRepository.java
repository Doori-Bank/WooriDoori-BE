package com.app.wooridooribe.repository.card;

import com.app.wooridooribe.entity.Card;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CardRepository extends JpaRepository<Card, Long> {

    @Query("SELECT c FROM Card c LEFT JOIN FETCH c.cardImage")
    List<Card> findAllWithImage();
}

package com.app.wooridooribe.repository.cardhistory;

import com.app.wooridooribe.entity.CardHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardHistoryRepository
        extends JpaRepository<CardHistory, Long>, CardHistoryQueryDsl {
}


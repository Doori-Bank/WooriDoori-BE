package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.entity.CardHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardHistoryRepository extends JpaRepository<CardHistory, Long>, CardHistoryQueryDsl {
    
    boolean existsByIdAndMemberCardMemberId(Long historyId, Long memberId);
}


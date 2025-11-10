package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long>, GoalQueryDsl {
    
    /**
     * 특정 회원의 가장 최근 목표를 조회합니다.
     */
    @Query("SELECT g FROM Goal g " +
           "WHERE g.member.id = :memberId " +
           "ORDER BY g.goalStartDate DESC")
    Optional<Goal> findLatestGoalByMemberId(@Param("memberId") Long memberId);
}


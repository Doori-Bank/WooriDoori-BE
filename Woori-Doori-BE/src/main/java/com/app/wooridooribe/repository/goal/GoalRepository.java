package com.app.wooridooribe.repository.goal;

import com.app.wooridooribe.entity.Goal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long>, GoalQueryDsl {
   // List<Goal> findAllGoalsByMember(String userName);

}


package com.app.dooribankbe.domain.repository;

import com.app.dooribankbe.domain.entity.AccountHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountHistoryRepository extends JpaRepository<AccountHistory, Long> {
    
    // AccountHistory -> MemberAccount -> DB2Member (tbl_member) 조인
    // db2 트랜잭션 내에서 필요한 모든 연관 엔티티를 함께 조회
    @Query("SELECT DISTINCT ah FROM AccountHistory ah " +
           "JOIN FETCH ah.account a " +
           "JOIN FETCH a.DB2Member m " +
           "WHERE ah.id = :id")
    Optional<AccountHistory> findByIdWithAccountAndMember(@Param("id") Long id);
}


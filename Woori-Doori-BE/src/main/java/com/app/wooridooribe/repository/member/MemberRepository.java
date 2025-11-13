package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberId(String memberId);

    boolean existsByMemberId(String memberEmail);

    Optional<Member> findByMemberNameAndPhone(String name, String phone);
    
    /**
     * 3개월 내 로그인한 유저 조회
     * @param threeMonthsAgo 3개월 전 날짜
     * @return 로그인한 유저 목록
     */
    @Query("SELECT m FROM Member m WHERE m.lastLoginDate >= :threeMonthsAgo OR m.lastLoginDate IS NULL")
    List<Member> findMembersLoggedInWithinThreeMonths(@Param("threeMonthsAgo") LocalDateTime threeMonthsAgo);
}
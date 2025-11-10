package com.app.wooridooribe.repository.membercard;

import com.app.wooridooribe.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MemberCardRepository extends JpaRepository<MemberCard, Long> {

    @Query("SELECT mc FROM MemberCard mc JOIN FETCH mc.card c LEFT JOIN FETCH c.cardImage WHERE mc.member.id = :memberId")
    List<MemberCard> findByMemberIdWithCard(@Param("memberId") Long memberId);

    @Query("SELECT mc FROM MemberCard mc WHERE mc.member.id = :memberId")
    List<MemberCard> findByMemberId(@Param("memberId") Long memberId);

    @Query("SELECT mc FROM MemberCard mc JOIN FETCH mc.card c LEFT JOIN FETCH c.cardImage WHERE mc.member.id = :memberId AND mc.cardNum = :cardNum")
    java.util.Optional<MemberCard> findByMemberIdAndCardNum(@Param("memberId") Long memberId,
            @Param("cardNum") String cardNum);

    @Query("SELECT mc FROM MemberCard mc JOIN FETCH mc.card c LEFT JOIN FETCH c.cardImage WHERE mc.cardNum = :cardNum")
    java.util.Optional<MemberCard> findByCardNum(@Param("cardNum") String cardNum);

    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE tbl_member_card SET member_id = :memberId WHERE id = :memberCardId", nativeQuery = true)
    int updateMemberId(@Param("memberCardId") Long memberCardId, @Param("memberId") Long memberId);

    @Modifying
    @Query(value = "UPDATE tbl_member_card SET member_id = NULL WHERE id = :memberCardId AND member_id = :memberId", nativeQuery = true)
    int deleteMemberId(@Param("memberCardId") Long memberCardId, @Param("memberId") Long memberId);
}

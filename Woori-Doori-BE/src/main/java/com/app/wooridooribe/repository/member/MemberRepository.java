package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberQueryDsl {

    Optional<Member> findByMemberId(String memberId);

    List<Member> findAllByMemberName(String memberName);

    Optional<Member> findByMemberNameAndPhone(String name, String phone);

    /**
     * 이름, 생년월일(앞 6자리), 뒷자리(1자리), 전화번호로 Member를 조회합니다.
     * 주민번호 대조를 위해 사용됩니다.
     */
    @Query("SELECT m FROM Member m WHERE m.memberName = :name AND m.birthDate = :birthDate AND m.birthBack = :birthBack AND m.phone = :phone")
    Optional<Member> findByMemberNameAndBirthDateAndBirthBackAndPhone(
            @Param("name") String name,
            @Param("birthDate") String birthDate,
            @Param("birthBack") String birthBack,
            @Param("phone") String phone
    );

    /**
     * 로그인 실패 횟수 증가 및 잠금 상태 업데이트 (Native Query로 직접 UPDATE)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE tbl_member SET failed_attempts = :failedAttempts, lock_level = :lockLevel, locked_until = :lockedUntil WHERE member_id = :memberId", nativeQuery = true)
    int updateLoginFailure(@Param("memberId") String memberId, 
                          @Param("failedAttempts") int failedAttempts,
                          @Param("lockLevel") int lockLevel,
                          @Param("lockedUntil") LocalDateTime lockedUntil);

}

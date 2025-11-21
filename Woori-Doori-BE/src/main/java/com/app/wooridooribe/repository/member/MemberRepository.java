package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberQueryDsl {

    Optional<Member> findByMemberId(String memberId);

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
}

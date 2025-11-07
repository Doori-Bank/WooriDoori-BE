package com.app.wooridooribe.repository.member;

import com.app.wooridooribe.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberId(String memberEmail);

    boolean existsByMemberId(String memberEmail);

    Optional<Member> findByMemberNameAndPhone(String name, String phone);
}
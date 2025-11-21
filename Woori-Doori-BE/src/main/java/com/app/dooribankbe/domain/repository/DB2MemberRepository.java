package com.app.dooribankbe.domain.repository;

import com.app.dooribankbe.domain.entity.DB2Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DB2MemberRepository extends JpaRepository<DB2Member, Long> {
    Optional<DB2Member> findByNameAndPhone(String name, String phone);
    Optional<DB2Member> findByMemberRegistNum(String memberRegistNum);
}


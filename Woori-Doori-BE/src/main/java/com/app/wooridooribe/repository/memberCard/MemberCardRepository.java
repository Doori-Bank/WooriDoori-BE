package com.app.wooridooribe.repository.memberCard;

import com.app.wooridooribe.entity.MemberCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberCardRepository extends JpaRepository<MemberCard, Long>, MemberCardQueryDSL {
}

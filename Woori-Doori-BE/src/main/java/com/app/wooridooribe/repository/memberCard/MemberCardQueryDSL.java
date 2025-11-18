package com.app.wooridooribe.repository.memberCard;

import com.app.wooridooribe.entity.MemberCard;
import org.springframework.data.repository.NoRepositoryBean;
import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface MemberCardQueryDSL {

    List<MemberCard> findMemberCardsByMemberId(Long memberId);

    Optional<MemberCard> findByMemberIdAndCardNum(Long memberId, String cardNum);

    Optional<MemberCard> findByCardNum(String cardNum);
}

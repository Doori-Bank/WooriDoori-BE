package com.app.wooridooribe.repository.membercard;

import com.app.wooridooribe.entity.MemberCard;
import java.util.List;
import java.util.Optional;

public interface MemberCardRepositoryCustom {

    List<MemberCard> findByMemberIdWithCard(Long memberId);

    Optional<MemberCard> findByMemberIdAndCardNum(Long memberId, String cardNum);

    Optional<MemberCard> findByCardNum(String cardNum);
}

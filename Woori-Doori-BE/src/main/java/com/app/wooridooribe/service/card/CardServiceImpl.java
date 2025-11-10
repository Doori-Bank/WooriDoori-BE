package com.app.wooridooribe.service.card;

import com.app.wooridooribe.controller.dto.CardCreateRequestDto;
import com.app.wooridooribe.controller.dto.CardDeleteRequestDto;
import com.app.wooridooribe.controller.dto.CardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardResponseDto;
import com.app.wooridooribe.controller.dto.UserCardResponseDto;
import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.card.CardRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.membercard.MemberCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {

    private final MemberCardRepository memberCardRepository;
    private final CardRepository cardRepository;
    private final MemberRepository memberRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardList(Long memberId) {
        List<MemberCard> memberCards = memberCardRepository.findByMemberIdWithCard(memberId);

        return memberCards.stream()
                .map(CardResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getAllCards() {
        List<Card> cards = cardRepository.findAllWithImage();

        return cards.stream()
                .map(CardResponseDto::fromCard)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserCardResponseDto createUserCard(Long memberId, CardCreateRequestDto request) {
        Long safeMemberId = Objects.requireNonNull(memberId, "memberId must not be null");
        String cardNum = Objects.requireNonNull(request.getCardNum(), "cardNum must not be null");

        // Member 엔티티 참조 (프록시 객체 - FK 설정용)
        Member member = memberRepository.getReferenceById(safeMemberId);

        // 기존 카드가 있는지 확인 (카드번호로만 검색 - member가 null일 수 있으므로)
        Optional<MemberCard> existingCard = memberCardRepository.findByCardNum(cardNum);

        MemberCard memberCard;
        if (existingCard.isPresent()) {
            // 기존 카드가 있으면 검증 후 card_alias만 업데이트
            memberCard = existingCard.get();

            // 카드 정보 검증 (비밀번호, 유효기간, 주민등록번호)
            if (!memberCard.getCardPw().equals(request.getCardPw())) {
                throw new CustomException(ErrorCode.INVALID_CARD_PASSWORD);
            }
            if (!memberCard.getExpiryMmYy().equals(request.getExpiryMmYy())) {
                throw new CustomException(ErrorCode.CARD_EXPIRED);
            }
            // 주민등록번호 앞자리와 뒷자리 각각 검증
            log.debug("주민등록번호 검증 - DB: registNum={}, registBack={}, 요청: registNum={}, registBack={}",
                    memberCard.getCardUserRegistNum(), memberCard.getCardUserRegistBack(),
                    request.getCardUserRegistNum(), request.getCardUserRegistBack());

            if (!memberCard.getCardUserRegistNum().equals(request.getCardUserRegistNum())) {
                log.warn("주민등록번호 앞자리 불일치 - DB: {}, 요청: {}",
                        memberCard.getCardUserRegistNum(), request.getCardUserRegistNum());
                throw new CustomException(ErrorCode.INVALID_BIRTHDATE);
            }
            if (!memberCard.getCardUserRegistBack().equals(request.getCardUserRegistBack())) {
                log.warn("주민등록번호 뒷자리 불일치 - DB: {}, 요청: {}",
                        memberCard.getCardUserRegistBack(), request.getCardUserRegistBack());
                throw new CustomException(ErrorCode.INVALID_BIRTHDATE);
            }
            // CVC 검증
            if (!memberCard.getCardCvc().equals(request.getCardCvc())) {
                log.warn("CVC 불일치 - DB: {}, 요청: {}",
                        memberCard.getCardCvc(), request.getCardCvc());
                throw new CustomException(ErrorCode.INVALID_CVC);
            }

            // member FK 설정 및 card_alias 업데이트 후 DB에 저장
            log.info("MemberCard 업데이트 전 - memberId: {}, cardNum: {}, 기존 member: {}",
                    safeMemberId, cardNum,
                    memberCard.getMember() != null ? memberCard.getMember().getId() : "null");

            memberCard.setMember(member);

            // card_alias 업데이트
            if (request.getCardAlias() != null && !request.getCardAlias().isEmpty()) {
                memberCard.setCardAlias(request.getCardAlias());
            }

            memberCardRepository.save(memberCard);

            memberCard = memberCardRepository.findByMemberIdAndCardNum(safeMemberId, cardNum)
                    .orElse(memberCard);

            log.info("최종 조회 후 - memberCard member: {}",
                    memberCard.getMember() != null ? memberCard.getMember().getId() : "null");
        } else {
            // 기존 카드가 없으면 에러 (카드번호로 Card를 식별할 수 없으므로)
            throw new CustomException(ErrorCode.CARD_ISNULL);
        }

        return UserCardResponseDto.from(memberCard);
    }

    @Override
    @Transactional
    public void deleteCard(Long memberId, CardDeleteRequestDto request) {
        Long safeMemberId = Objects.requireNonNull(memberId, "memberId must not be null");
        Long cardId = Objects.requireNonNull(request.getId(), "cardId must not be null");

        // 해당 사용자의 카드 찾기 (id와 memberId로 검증)
        MemberCard card = memberCardRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_ISNULL));

        // 본인의 카드인지 확인
        if (card.getMember() == null || !card.getMember().getId().equals(safeMemberId)) {
            throw new CustomException(ErrorCode.CARD_ISNOTYOURS);
        }

        // member_id를 NULL로 설정 (화면에서만 삭제, DB에는 남아있음)
        card.setMember(null);
        memberCardRepository.save(card);

        log.info("카드 삭제 완료 - memberCardId: {}, memberId: {}", cardId, safeMemberId);
    }

    @Override
    @Transactional
    public void editCardAlias(Long memberId, CardEditRequestDto request) {
        Long safeMemberId = Objects.requireNonNull(memberId, "memberId must not be null");
        Long cardId = Objects.requireNonNull(request.getId(), "cardId must not be null");

        // member_id가 등록되어 있는 카드 찾기 (id로 조회)
        MemberCard memberCard = memberCardRepository.findById(cardId)
                .orElseThrow(() -> new CustomException(ErrorCode.CARD_ISNULL));

        // 본인의 카드인지 확인
        if (memberCard.getMember() == null || !memberCard.getMember().getId().equals(safeMemberId)) {
            throw new CustomException(ErrorCode.CARD_ISNOTYOURS);
        }

        // card_alias 업데이트
        memberCard.setCardAlias(request.getCardAlias());
        memberCardRepository.saveAndFlush(memberCard);

        log.info("카드 별명 수정 완료 - memberCardId: {}, memberId: {}, cardAlias: {}",
                cardId, safeMemberId, request.getCardAlias());
    }
}

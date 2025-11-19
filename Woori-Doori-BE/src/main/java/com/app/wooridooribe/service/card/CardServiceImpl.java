package com.app.wooridooribe.service.card;

import com.app.wooridooribe.controller.dto.AdminCardCreateRequestDto;
import com.app.wooridooribe.controller.dto.AdminCardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardCreateRequestDto;
import com.app.wooridooribe.controller.dto.CardDeleteRequestDto;
import com.app.wooridooribe.controller.dto.CardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardRecommendResponseDto;
import com.app.wooridooribe.controller.dto.CardResponseDto;
import com.app.wooridooribe.controller.dto.UserCardResponseDto;
import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.File;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.repository.card.CardRepository;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.file.FileRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.memberCard.MemberCardRepository;
import com.app.wooridooribe.service.s3FileService.S3FileService;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
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
    private final CardHistoryRepository cardHistoryRepository;
    private final FileRepository fileRepository;
    private final S3FileService s3FileService;

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getCardList(Long memberId) {
        List<MemberCard> memberCards = memberCardRepository.findMemberCardsByMemberId(memberId);

        return memberCards.stream()
                .map(CardResponseDto::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardResponseDto> getAllCards() {
        List<Card> cards = cardRepository.findAllWithImage();

        return cards.stream()
                .map(CardResponseDto::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CardResponseDto createCardForAdmin(AdminCardCreateRequestDto request) {
        log.info("관리자 - 카드 생성 요청 수신: cardName={}", request.getCardName());

        Long cardImageFileId = Objects.requireNonNull(request.getCardImageFileId(), "cardImageFileId must not be null");
        File cardImage = fileRepository.findById(cardImageFileId)
                .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));

        File cardBanner = null;
        Long cardBannerFileId = request.getCardBannerFileId();
        if (cardBannerFileId != null) {
            cardBanner = fileRepository.findById(cardBannerFileId)
                    .orElseThrow(() -> new CustomException(ErrorCode.FILE_NOT_FOUND));
        }

        Card newCard = Card.builder()
                .cardName(request.getCardName())
                .cardBenefit(request.getCardBenefit())
                .cardSvc(request.getCardSvc())
                .annualFee1(request.getAnnualFee1())
                .annualFee2(request.getAnnualFee2())
                .cardType(request.getCardType())
                .cardImage(cardImage)
                .cardBanner(cardBanner)
                .build();

        Card savedCard = cardRepository.save(newCard);
        log.info("관리자 - 카드 생성 완료: cardId={}, cardName={}", savedCard.getId(), savedCard.getCardName());

        return CardResponseDto.toDTO(savedCard);
    }

    @Override
    @Transactional
    public CardResponseDto editCardForAdmin(AdminCardEditRequestDto request) {
        Long cardId = Objects.requireNonNull(request.getCardId(), "cardId must not be null");
        log.info("관리자 - 카드 수정 요청 수신: cardId={}", cardId);

        // 카드 조회
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> {
                    log.warn("관리자 - 카드 수정 실패: 카드를 찾을 수 없음 - cardId={}", cardId);
                    return new CustomException(ErrorCode.CARD_ISNULL);
                });

        // 카드명 수정
        if (request.getCardName() != null && !request.getCardName().isEmpty()) {
            card.setCardName(request.getCardName());
        }

        // 연회비 수정
        if (request.getAnnualFee1() != null) {
            card.setAnnualFee1(request.getAnnualFee1());
        }
        if (request.getAnnualFee2() != null) {
            card.setAnnualFee2(request.getAnnualFee2());
        }

        // 카드 혜택 수정
        if (request.getCardBenefit() != null) {
            card.setCardBenefit(request.getCardBenefit());
        }

        // 카드 타입 수정
        if (request.getCardType() != null) {
            card.setCardType(request.getCardType());
        }

        // 서비스 여부 수정
        if (request.getCardSvc() != null) {
            card.setCardSvc(request.getCardSvc());
        }

        // 카드 이미지 수정
        if (request.getCardImageFileId() != null) {
            Long cardImageFileId = Objects.requireNonNull(request.getCardImageFileId(),
                    "cardImageFileId must not be null");
            File cardImage = fileRepository.findById(cardImageFileId)
                    .orElseThrow(() -> {
                        log.warn("관리자 - 카드 수정 실패: 카드 이미지 파일을 찾을 수 없음 - fileId={}", cardImageFileId);
                        return new CustomException(ErrorCode.FILE_NOT_FOUND);
                    });
            card.setCardImage(cardImage);
        }

        // 카드 배너 수정
        if (request.getCardBannerFileId() != null) {
            Long cardBannerFileId = Objects.requireNonNull(request.getCardBannerFileId(),
                    "cardBannerFileId must not be null");
            File cardBanner = fileRepository.findById(cardBannerFileId)
                    .orElseThrow(() -> {
                        log.warn("관리자 - 카드 수정 실패: 카드 배너 파일을 찾을 수 없음 - fileId={}", cardBannerFileId);
                        return new CustomException(ErrorCode.FILE_NOT_FOUND);
                    });
            card.setCardBanner(cardBanner);
        }

        Card savedCard = Objects.requireNonNull(cardRepository.save(card), "savedCard must not be null");
        log.info("관리자 - 카드 수정 완료: cardId={}, cardName={}", savedCard.getId(), savedCard.getCardName());

        return CardResponseDto.toDTO(savedCard);
    }

    @Override
    @Transactional
    public void deleteCardForAdmin(Long cardId) {
        Long safeCardId = Objects.requireNonNull(cardId, "cardId must not be null");
        log.info("관리자 - 카드 삭제 요청 수신: cardId={}", safeCardId);

        Card card = cardRepository.findById(safeCardId)
                .orElseThrow(() -> {
                    log.warn("관리자 - 카드 삭제 실패: 카드를 찾을 수 없음 - cardId={}", safeCardId);
                    return new CustomException(ErrorCode.CARD_ISNULL);
                });

        // 카드 이미지 파일 삭제 (S3 및 DB)
        File cardImage = card.getCardImage();
        if (cardImage != null) {
            try {
                // S3에서 파일 삭제
                boolean s3Deleted = s3FileService.deleteImage(cardImage.getUuid());
                if (s3Deleted) {
                    log.info("관리자 - 카드 이미지 S3 삭제 완료: fileId={}, uuid={}", cardImage.getId(), cardImage.getUuid());
                } else {
                    log.warn("관리자 - 카드 이미지 S3 삭제 실패: fileId={}, uuid={}", cardImage.getId(), cardImage.getUuid());
                }
                // DB에서 File 엔티티 삭제
                fileRepository.delete(cardImage);
                log.info("관리자 - 카드 이미지 File 엔티티 삭제 완료: fileId={}", cardImage.getId());
            } catch (Exception e) {
                log.error("관리자 - 카드 이미지 삭제 중 오류 발생: fileId={}, error={}", cardImage.getId(), e.getMessage(), e);
            }
        }

        // 카드 배너 이미지 파일 삭제 (S3 및 DB)
        File cardBanner = card.getCardBanner();
        if (cardBanner != null) {
            try {
                // S3에서 파일 삭제
                boolean s3Deleted = s3FileService.deleteImage(cardBanner.getUuid());
                if (s3Deleted) {
                    log.info("관리자 - 카드 배너 이미지 S3 삭제 완료: fileId={}, uuid={}", cardBanner.getId(), cardBanner.getUuid());
                } else {
                    log.warn("관리자 - 카드 배너 이미지 S3 삭제 실패: fileId={}, uuid={}", cardBanner.getId(), cardBanner.getUuid());
                }
                // DB에서 File 엔티티 삭제
                fileRepository.delete(cardBanner);
                log.info("관리자 - 카드 배너 이미지 File 엔티티 삭제 완료: fileId={}", cardBanner.getId());
            } catch (Exception e) {
                log.error("관리자 - 카드 배너 이미지 삭제 중 오류 발생: fileId={}, error={}", cardBanner.getId(), e.getMessage(), e);
            }
        }

        // 카드 삭제 (하드 삭제)
        cardRepository.delete(card);

        log.info("관리자 - 카드 삭제 완료: cardId={}, cardName={}", safeCardId, card.getCardName());
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

    @Override
    @Transactional(readOnly = true)
    public CardRecommendResponseDto recommendCards(Long memberId) {
        log.info("카드 추천 요청 - memberId: {}", memberId);

        // 1. 현재 년/월 기준으로 사용자의 결제 내역에서 카테고리별 소비 금액 조회
        LocalDate today = LocalDate.now();
        LocalDate startDate = YearMonth.from(today).atDay(1); // 이번 달 1일
        LocalDate endDate = YearMonth.from(today).atEndOfMonth(); // 이번 달 마지막 날

        // 2. 카테고리별 소비 금액 조회 (TOP 1 추출)
        List<Tuple> categorySpendingList = cardHistoryRepository
                .getCategorySpendingByMemberAndDateRange(memberId, startDate, endDate);

        if (categorySpendingList == null || categorySpendingList.isEmpty()) {
            log.warn("카드 추천 - 결제 내역이 없습니다. memberId: {}", memberId);
            return CardRecommendResponseDto.builder()
                    .topCategory(null)
                    .cards(List.of())
                    .build();
        }

        // 3. TOP 1 카테고리 추출
        Tuple topCategoryTuple = categorySpendingList.get(0);
        CategoryType topCategory = topCategoryTuple.get(0, CategoryType.class);

        if (topCategory == null) {
            log.warn("카드 추천 - TOP 카테고리를 찾을 수 없습니다. memberId: {}", memberId);
            return CardRecommendResponseDto.builder()
                    .topCategory(null)
                    .cards(List.of())
                    .build();
        }

        log.info("카드 추천 - TOP 카테고리: {}, memberId: {}", topCategory, memberId);

        // 4. 여러 카테고리를 순차적으로 확인하여 최대 4개까지 카드 수집
        List<Long> allCardIds = new ArrayList<>();
        List<CategoryType> categories = new ArrayList<>();

        // 최대 5개 카테고리까지 확인 (4개가 될 때까지)
        int maxCategories = Math.min(5, categorySpendingList.size());
        for (int i = 0; i < maxCategories && allCardIds.size() < 4; i++) {
            Tuple categoryTuple = categorySpendingList.get(i);
            CategoryType category = categoryTuple.get(0, CategoryType.class);

            if (category == null) {
                continue;
            }

            // 필요한 카드 개수 계산
            int neededCount = 4 - allCardIds.size();

            // 해당 카테고리에서 인기 카드 조회 (이미 추가된 카드 제외)
            // 사용 횟수가 0인 카드도 포함하여 최대한 4개를 채움
            List<Tuple> popularCardsWithCount = cardRepository.findPopularCardsByCategory(
                    category, allCardIds, neededCount);

            if (popularCardsWithCount != null && !popularCardsWithCount.isEmpty()) {
                // 필요한 개수만큼만 추가
                int remainingNeeded = 4 - allCardIds.size();
                List<Long> categoryCardIds = popularCardsWithCount.stream()
                        .limit(remainingNeeded)
                        .map(tuple -> tuple.get(0, Long.class))
                        .collect(Collectors.toList());

                if (!categoryCardIds.isEmpty()) {
                    allCardIds.addAll(categoryCardIds);
                    categories.add(category);

                    log.info("카드 추천 - 카테고리: {}, 추가된 카드 ID: {}, 현재 총 카드 수: {}",
                            category, categoryCardIds, allCardIds.size());

                    // 4개가 되면 중단
                    if (allCardIds.size() >= 4) {
                        break;
                    }
                }
            }
        }

        if (allCardIds.isEmpty()) {
            log.warn("카드 추천 - 추천할 카드가 없습니다. memberId: {}", memberId);
            return CardRecommendResponseDto.builder()
                    .topCategory(topCategory)
                    .cards(List.of())
                    .build();
        }

        // 5. 최대 4개까지만 유지
        allCardIds = allCardIds.stream().limit(4).collect(Collectors.toList());

        log.info("카드 추천 결과 - TOP 카테고리: {}, 사용된 카테고리: {}, 최종 카드 ID: {}, 카드 수: {}",
                topCategory, categories, allCardIds, allCardIds.size());

        // 6. 카드 정보 조회 (ID 순서 유지)
        List<Card> recommendedCards = cardRepository.findCardsByIdIn(allCardIds);

        // 7. ID 순서대로 정렬 (전체 사용자 사용 횟수 순서 유지)
        List<Card> sortedCards = allCardIds.stream()
                .map(cardId -> recommendedCards.stream()
                        .filter(card -> card.getId().equals(cardId))
                        .findFirst()
                        .orElse(null))
                .filter(card -> card != null)
                .collect(Collectors.toList());

        // 8. CardResponseDto로 변환
        List<CardResponseDto> cardResponseDtos = sortedCards.stream()
                .map(CardResponseDto::toDTO)
                .collect(Collectors.toList());

        // 9. CardRecommendResponseDto 생성 및 반환
        return CardRecommendResponseDto.builder()
                .topCategory(topCategory)
                .cards(cardResponseDtos)
                .build();
    }
}

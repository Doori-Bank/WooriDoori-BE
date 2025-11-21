package com.app.wooridooribe.service.main;

import com.app.wooridooribe.controller.dto.CardRecommendDto;
import com.app.wooridooribe.controller.dto.CategorySpendDto;
import com.app.wooridooribe.controller.dto.MainDto;
import com.app.wooridooribe.entity.Card;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.card.CardRepository;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.goal.GoalRepository;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MainServiceImpl implements MainService {

    private final GoalRepository goalRepository;
    private final CardHistoryRepository cardHistoryRepository;
    private final CardRepository cardRepository;

    @Override
    public MainDto getMainPageData(Long memberId) {
        // 현재 로그인한 사용자의 ID 가져오기
        log.info("메인 페이지 데이터 조회 시작 - memberId: {}", memberId);

        // 1. 이번 달 목표 조회 (QueryDSL)
        Goal latestGoal = goalRepository.findCurrentMonthGoalByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));

        LocalDate goalStartDate = latestGoal.getGoalStartDate();
        LocalDate today = LocalDate.now();
        
        // 목표 기간: 시작일로부터 30일 (한 달)
        LocalDate goalEndDate = goalStartDate.plusDays(30);
        
        // 2. 날짜 계산
        int fullDate = 30; // 전체 목표 기간
        int duringDate = (int) ChronoUnit.DAYS.between(goalStartDate, today);
        if (duringDate > fullDate) {
            duringDate = fullDate;
        }
        if (duringDate < 0) {
            duringDate = 0;
        }

        // 3. 총 지출 금액 조회 (QueryDSL)
        Integer totalPaidMoney = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                memberId, goalStartDate, today
        );
        // 데이터가 없을 경우 0으로 설정
        if (totalPaidMoney == null) {
            totalPaidMoney = 0;
        }

        // 4. 목표 달성률 계산
        Integer goalMoney = latestGoal.getPreviousGoalMoney();
        
        Integer goalPercent = null;
        if (goalMoney != null && goalMoney > 0 && totalPaidMoney != null) {
            // 목표 금액은 만원 단위이므로 원 단위로 변환하여 비교
            int goalMoneyInWon = goalMoney * 10000;
            goalPercent = (int) Math.round((double) totalPaidMoney / goalMoneyInWon * 100);
            goalPercent = Math.min(100, Math.max(0, goalPercent)); // 0~100 범위 제한
        }

        // 5. 예상 남은 일수 계산 (현재 페이스로 목표 금액을 초과하는 예상 일수)
        Integer remainingDays = null;
        if (duringDate > 0 && totalPaidMoney != null && totalPaidMoney > 0 && goalMoney != null && goalMoney > 0) {
            int goalMoneyInWon = goalMoney * 10000;
            int remainingMoney = goalMoneyInWon - totalPaidMoney; // 남은 금액
            
            if (remainingMoney > 0) {
                double dailyAvg = (double) totalPaidMoney / duringDate; // 하루 평균 사용 금액
                if (dailyAvg > 0) {
                    remainingDays = (int) Math.ceil((double) remainingMoney / dailyAvg); // 남은 금액을 사용할 수 있는 예상 일수
                }
            } else {
                // 이미 목표 금액을 초과한 경우
                remainingDays = 0;
            }
        }

        // 6. 카테고리별 지출 TOP 5 조회 (QueryDSL)
        List<Tuple> categorySpendingList = cardHistoryRepository.getCategorySpendingByMemberAndDateRange(
                memberId, goalStartDate, today
        );
        
        List<CategorySpendDto> paidPriceOfCategory = new ArrayList<>();
        String[] ranks = {"top1", "top2", "top3", "top4", "top5"};
        for (int i = 0; i < Math.min(categorySpendingList.size(), 5); i++) {
            Tuple tuple = categorySpendingList.get(i);
            CategoryType categoryType = tuple.get(0, CategoryType.class);
            Integer totalPrice = tuple.get(1, Integer.class);
            
            // CategoryType Enum을 String으로 변환
            String category = categoryType != null ? categoryType.name() : "";
            
            paidPriceOfCategory.add(CategorySpendDto.builder()
                    .rank(ranks[i])
                    .category(category)
                    .totalPrice(totalPrice)
                    .build());
        }

        // 7. 가장 많이 사용한 카드 TOP 3 조회 및 카드 배너 정보 가져오기 (QueryDSL)
        List<Tuple> topUsedCards = cardHistoryRepository.getTopUsedCards();
        
        List<CardRecommendDto> cardRecommend = new ArrayList<>();
        if (!topUsedCards.isEmpty()) {
            // TOP 3까지만 가져오기
            List<Long> top3CardIds = topUsedCards.stream()
                    .limit(3)
                    .map(tuple -> tuple.get(0, Long.class))
                    .collect(Collectors.toList());
            
            // 카드 정보 조회 (배너 이미지 포함)
            List<Card> cards = cardRepository.findCardsByIdIn(top3CardIds);
            
            // 카드 ID 순서를 유지하면서 결과 생성
            for (Long cardId : top3CardIds) {
                Card card = cards.stream()
                        .filter(c -> c.getId().equals(cardId))
                        .findFirst()
                        .orElse(null);
                
                if (card != null) {
                    String bannerUrl = null;
                    if (card.getCardBanner() != null) {
                        bannerUrl = card.getCardBanner().getFilePath();
                    }
                    
                    cardRecommend.add(CardRecommendDto.builder()
                            .cardId(card.getId())
                            .cardBannerUrl(bannerUrl)
                            .build());
                }
            }
        }

        // 8. MainDto 생성 및 반환
        MainDto mainDto = MainDto.builder()
                .fullDate(fullDate)
                .duringDate(duringDate)
                .remainingDays(remainingDays)
                .goalPercent(goalPercent)
                .goalMoney(goalMoney)
                .totalPaidMoney(totalPaidMoney)
                .paidPriceOfCategory(paidPriceOfCategory)
                .cardRecommend(cardRecommend)
                .build();

        log.info("메인 페이지 데이터 조회 완료 - memberId: {}, totalPaidMoney: {}, goalPercent: {}%, remainingDays: {}일", 
                 memberId, totalPaidMoney, goalPercent, remainingDays);
        
        return mainDto;
    }
}


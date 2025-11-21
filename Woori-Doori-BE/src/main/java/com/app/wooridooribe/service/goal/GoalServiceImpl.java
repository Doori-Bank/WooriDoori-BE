package com.app.wooridooribe.service.goal;

import com.app.wooridooribe.controller.dto.*;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.CategoryMember;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.JobType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.goal.GoalRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.categoryMember.CategoryMemberRepository;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GoalServiceImpl implements GoalService {

    private final GoalRepository goalRepository;
    private final MemberRepository memberRepository;
    private final CardHistoryRepository cardHistoryRepository;
    private final CategoryMemberRepository categoryMemberRepository;
    
    // 필수 카테고리 목록 (필수/비필수 구분용)
    private static final List<CategoryType> ESSENTIAL_CATEGORIES = Arrays.asList(
            CategoryType.FOOD,           // 식비
            CategoryType.TRANSPORTATION, // 교통/자동차
            CategoryType.HOUSING,        // 주거
            CategoryType.HOSPITAL,       // 병원
            CategoryType.EDUCATION       // 교육
    );

    @Override
    public GoalResponseDto setGoal(Long memberId, SetGoalDto setGoalDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (setGoalDto == null ||
                setGoalDto.getGoalJob() == null ||
                setGoalDto.getGoalIncome() == null ||
                setGoalDto.getPreviousGoalMoney() == null) {
            throw new CustomException(ErrorCode.GOAL_INVALIDVALUE);
        }

        if((Integer.parseInt(setGoalDto.getGoalIncome())<setGoalDto.getPreviousGoalMoney())
                && (!(setGoalDto.getGoalJob().equals(JobType.UNEMPLOYED) || setGoalDto.getGoalJob().equals(JobType.STUDENT)))) {
            //제한금액이 급여보다 클 경우
            throw new CustomException(ErrorCode.GOAL_INVALIDNUM);
        }

        //이번 달과 다음 달 목표 조회 (QueryDSL)
        List<Goal> goals = goalRepository.findGoalsForThisAndNextMonth(member);
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(1);
        LocalDate nextMonth = thisMonth.plusMonths(1);

        boolean thisMonthGoalExists = goals.stream()
                .anyMatch(g -> g.getGoalStartDate().equals(thisMonth));
        boolean nextMonthGoalExists = goals.stream()
                .anyMatch(g -> g.getGoalStartDate().equals(nextMonth));


        // 사용자가 선택한 필수 카테고리 목록 (nullable)
        List<CategoryType> essentialCategoriesFromRequest = setGoalDto.getEssentialCategories();

        if (!thisMonthGoalExists) {
            // 이번 달 목표 등록
            setGoalDto.setGoalStartDate(thisMonth);
            Goal goal = setGoalDto.toEntity();
            goal.setMember(member);
            // 점수는 아직 계산하지 않고 0으로 초기화
            goal.setGoalAchievementScore(0);
            goal.setGoalStabilityScore(0);
            goal.setGoalRatioScore(0);
            goal.setGoalContinuityScore(0);
            goalRepository.save(goal);
            
            // 목표 설정 시 필수 카테고리 설정이 함께 들어온 경우 처리
            if (essentialCategoriesFromRequest != null) {
                updateEssentialCategoriesForMember(member, essentialCategoriesFromRequest);
            }
        } else if (!nextMonthGoalExists) {
            // 다음 달 목표 등록 (아직 시작 전이므로 점수 계산하지 않음)
            setGoalDto.setGoalStartDate(nextMonth);
            Goal goal = setGoalDto.toEntity();
            goal.setMember(member);
            // 점수는 아직 계산하지 않고 0으로 초기화
            goal.setGoalAchievementScore(0);
            goal.setGoalStabilityScore(0);
            goal.setGoalRatioScore(0);
            goal.setGoalContinuityScore(0);
            goalRepository.save(goal);
            
            if (essentialCategoriesFromRequest != null) {
                updateEssentialCategoriesForMember(member, essentialCategoriesFromRequest);
            }
        } else {
            // 다음 달 목표 수정
            setGoalDto.setGoalStartDate(nextMonth);
            Goal goal = setGoalDto.toEntity();
            goal.setMember(member);
            goal.setId(goals.stream().filter(g -> g.getGoalStartDate().equals(nextMonth)).findFirst().get().getId());
            // 점수는 아직 계산하지 않고 0으로 초기화
            goal.setGoalAchievementScore(0);
            goal.setGoalStabilityScore(0);
            goal.setGoalRatioScore(0);
            goal.setGoalContinuityScore(0);
            goalRepository.save(goal);
            
            if (essentialCategoriesFromRequest != null) {
                updateEssentialCategoriesForMember(member, essentialCategoriesFromRequest);
            }
        }

        return GoalResponseDto.builder()
                .thisMonthGoalExists(thisMonthGoalExists)
                .nextMonthGoalExists(nextMonthGoalExists)
                .goalData(setGoalDto)
                .build();
    }
    @Override
    public GoalScoreResponseDto calculateAndUpdateGoalScores(Long memberId) {
        // 1. 이번 달 목표 조회
        Goal currentGoal = goalRepository.findCurrentMonthGoalByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));

        LocalDate startDate = currentGoal.getGoalStartDate();
        
        // 점수 계산 및 업데이트
        return calculateGoalScoresForMonth(memberId, startDate, currentGoal);
    }
    
    /**
     * 회원의 필수 카테고리 설정 및 해당 카테고리 결제 내역 총지출 미포함 처리
     * (목표 설정 시 함께 호출)
     */
    private void updateEssentialCategoriesForMember(Member member, List<CategoryType> essentialCategories) {
        Long memberId = member.getId();

        // 기존 매핑 삭제
        categoryMemberRepository.deleteByMember(member);

        // 새 매핑 저장
        List<CategoryMember> newMappings = essentialCategories.stream()
                .map(cat -> CategoryMember.builder()
                        .member(member)
                        .categoryType(cat)
                        .build())
                .toList();
        categoryMemberRepository.saveAll(newMappings);

        // 해당 카테고리의 결제내역을 총지출에서 제외(N) 처리
        if (!essentialCategories.isEmpty()) {
            cardHistoryRepository.updateIncludeTotalByMemberAndCategories(memberId, essentialCategories, false);
        }
    }
    
    /**
     * 배치 작업용: 점수 계산만 하고 SSE 알림은 전송하지 않음
     */
    @Override
    @Transactional
    public GoalScoreResponseDto calculateAndUpdateGoalScoresBatch(Long memberId) {
        Goal currentGoal = goalRepository.findCurrentMonthGoalByMemberId(memberId)
                .orElse(null);
        
        if (currentGoal == null) {
            log.debug("목표가 없어서 점수 계산 스킵 - memberId: {}", memberId);
            return null;
        }

        LocalDate startDate = currentGoal.getGoalStartDate();
        
        // 점수 계산 및 업데이트
        return calculateGoalScoresForMonth(memberId, startDate, currentGoal);
    }
    
    /**
     * @param memberId 회원 ID
     * @param goalStartDate 목표 시작일 (월의 첫날)
     * @param goal 계산할 Goal 엔티티 (null이면 조회)
     * @return 계산된 점수 DTO
     */
    private GoalScoreResponseDto calculateGoalScoresForMonth(Long memberId, LocalDate goalStartDate, Goal goal) {
        // Goal이 null이면 조회
        if (goal == null) {
            goal = goalRepository.findGoalByMemberIdAndStartDate(memberId, goalStartDate)
                    .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));
        }

        LocalDate startDate = goal.getGoalStartDate();
        LocalDate endDate = startDate.plusDays(30);
        Integer goalMoneyInteger = goal.getPreviousGoalMoney();
        // 목표 금액은 만원 단위로 저장되어 있으므로 원 단위로 변환 (* 10000)
        int goalMoney = (goalMoneyInteger != null ? goalMoneyInteger : 0) * 10000;

        // 이번 달 지출 데이터 조회
        int actualSpending = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                memberId, startDate, endDate);

        // 일별 지출 조회 (안정성 계산용)
        List<Integer> dailySpending = cardHistoryRepository.getDailySpendingByMemberAndDateRange(
                memberId, startDate, endDate);

        // 필수/비필수 지출 조회
        List<Tuple> categorySpendingTuples = cardHistoryRepository
                .getEssentialNonEssentialSpending(memberId, startDate, endDate, ESSENTIAL_CATEGORIES);
        
        int essentialSpending = 0;
        int nonEssentialSpending = 0;
        
        for (Tuple tuple : categorySpendingTuples) {
            String isEssential = tuple.get(0, String.class);
            Integer amount = tuple.get(1, Integer.class);
            
            if ("essential".equals(isEssential)) {
                essentialSpending = amount != null ? amount : 0;
            } else if ("nonEssential".equals(isEssential)) {
                nonEssentialSpending = amount != null ? amount : 0;
            }
        }

        // 지난 달 데이터 조회 (지속성 계산용)
        LocalDate lastMonthStart = startDate.minusMonths(1);
        LocalDate lastMonthEnd = lastMonthStart.plusDays(30);

        Goal lastGoal = goalRepository.findGoalByMemberIdAndStartDate(memberId, lastMonthStart)
                .orElse(null);

        Integer lastMonthSpending = null;
        Integer lastMonthGoal = null;
        if (lastGoal != null) {
            lastMonthSpending = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                    memberId, lastMonthStart, lastMonthEnd);
            // 목표 금액은 만원 단위이므로 원 단위로 변환
            lastMonthGoal = lastGoal.getPreviousGoalMoney() != null ? lastGoal.getPreviousGoalMoney() * 10000 : null;
        }

        // 4가지 점수 계산
        int achievementScore = calculateAchievementScore(goalMoney, actualSpending);
        int stabilityScore = calculateStabilityScore(dailySpending);
        int ratioScore = calculateRatioScore(essentialSpending, nonEssentialSpending);
        int continuityScore = calculateContinuityScore(
                actualSpending, goalMoney, lastMonthSpending, lastMonthGoal);

        // Goal 엔티티에 저장
        goal.setGoalAchievementScore(achievementScore);
        goal.setGoalStabilityScore(stabilityScore);
        goal.setGoalRatioScore(ratioScore);
        goal.setGoalContinuityScore(continuityScore);

        goalRepository.save(goal);

        // 카테고리별 소비내역 조회
        List<Tuple> categorySpendingList = cardHistoryRepository
                .getCategorySpendingByMemberAndDateRange(memberId, startDate, endDate);
        
        Map<CategoryType, Integer> categorySpendingMap = categorySpendingList.stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(0, CategoryType.class),
                        tuple -> tuple.get(1, Integer.class) != null ? tuple.get(1, Integer.class) : 0
                ));

        // 총점 계산
        int totalScore = achievementScore + stabilityScore + ratioScore + continuityScore;

        // DTO 생성
        return GoalScoreResponseDto.builder()
                .achievementScore(achievementScore)
                .stabilityScore(stabilityScore)
                .ratioScore(ratioScore)
                .continuityScore(continuityScore)
                .totalScore(totalScore)
                .categorySpending(categorySpendingMap)
                .build();
    }
    
    /**
     * 모든 활성 유저의 점수를 배치로 계산
     * 3개월 내 로그인한 유저만 처리
     */
    @Override
    @Transactional
    public int calculateAllActiveUsersScores() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
        List<Member> activeMembers = memberRepository.findMembersLoggedInWithinThreeMonths(threeMonthsAgo);
        
        log.info("배치 점수 계산 시작 - 활성 유저 수: {}", activeMembers.size());
        
        int successCount = 0;
        int failCount = 0;
        
        for (Member member : activeMembers) {
            try {
                GoalScoreResponseDto result = calculateAndUpdateGoalScoresBatch(member.getId());
                if (result != null) {
                    successCount++;
                    log.debug("점수 계산 완료 - memberId: {}", member.getId());
                } else {
                    failCount++;
                    log.debug("목표 없음으로 스킵 - memberId: {}", member.getId());
                }
            } catch (Exception e) {
                failCount++;
                log.error("점수 계산 실패 - memberId: {}", member.getId(), e);
            }
        }
        
        log.info("배치 점수 계산 완료 - 성공: {}, 실패/스킵: {}", successCount, failCount);
        return successCount;
    }
    
    /**
     * 목표 달성도 점수 계산 (40점 만점)
     * - 기본 35점
     * - 절약하면 보너스 (35~40점), 초과하면 감점 (0~35점)
     * 
     * if G <= 0:
     *     goalScore = 40 if A == 0 else 0
     * else:
     *     saveRate = (G - A) / G     # 절약률(+면 절약, -면 초과)
     *     if saveRate >= 0:
     *         # 절약률 0~30% 사이: 선형 보너스
     *         bonus = min(saveRate / 0.3, 1)
     *         goalScore = 35 + 5 * bonus      # 35~40점 (절약할수록 가점)
     *     else:
     *         # 목표 초과: 초과율만큼 감점
     *         penalty = min(abs(saveRate) / 0.3, 1)
     *         goalScore = 35 * (1 - penalty)   # 0~35점 (초과할수록 감점)
     */
    private int calculateAchievementScore(int goalMoney, int actualSpending) {
        // G: 목표 금액, A: 실제 지출
        int G = goalMoney;
        int A = actualSpending;
        
        if (G <= 0) {
            return A == 0 ? 40 : 0;
        }
        
        // 절약률 계산 (양수면 절약, 음수면 초과)
        double saveRate = (double) (G - A) / G;
        
        if (saveRate >= 0) {
            // 절약한 경우: 35~40점
            double bonus = Math.min(saveRate / 0.3, 1.0);
            return (int) Math.round(35 + 5 * bonus);
        } else {
            // 목표 초과한 경우: 0~35점
            double penalty = Math.min(Math.abs(saveRate) / 0.3, 1.0);
            return (int) Math.round(35 * (1 - penalty));
        }
    }
    
    /**
     * 소비 안정성 점수 계산 (20점 만점)
     * - 일별 지출의 변동계수(CV)를 이용해 계산
     * - CV가 작을수록 안정적 (높은 점수)
     * 
     * mean = avg(D)      # 일별 평균
     * std  = stdev(D)    # 표준편차
     * if mean <= 0:
     *     stabilityScore = 0
     * else:
     *     cv = std / mean                   # 변동계수
     *     stabilityScore = 20 * (1 - min(cv, 1))   # CV=0 → 20점, CV=1↑ → 0점
     */
    private int calculateStabilityScore(List<Integer> dailySpending) {
        if (dailySpending == null || dailySpending.isEmpty()) {
            return 0;
        }
        
        // 평균 계산
        double mean = dailySpending.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        
        if (mean <= 0) {
            return 0;
        }
        
        // 표준편차 계산
        double variance = dailySpending.stream()
                .mapToDouble(spending -> Math.pow(spending - mean, 2))
                .average()
                .orElse(0.0);
        
        double std = Math.sqrt(variance);
        
        // 변동계수 (CV) = 표준편차 / 평균
        double cv = std / mean;
        
        // CV가 낮을수록 안정적 (CV=0 → 20점, CV≥1 → 0점)
        return (int) Math.round(20 * (1 - Math.min(cv, 1.0)));
    }
    
    /**
     * 필수/비필수 지출 비율 점수 (20점 만점)
     * - 필수 지출 비율이 0.8 이상이면 20점 만점
     * - 0~0.8 구간: 선형 상승
     * 
     * sumEX = E + X
     * if sumEX <= 0:
     *     ratioScore = 0
     * else:
     *     p = E / sumEX   # 필수지출 비율 (0~1)
     *     if p <= 0.8:
     *         ratioScore = 20 * (p / 0.8)    # 0~0.8 구간: 선형 상승 (0~20점)
     *     else:
     *         ratioScore = 20                # 0.8 이상이면 만점
     */
    private int calculateRatioScore(int essentialSpending, int nonEssentialSpending) {
        // E: 필수 지출, X: 비필수 지출
        int E = essentialSpending;
        int X = nonEssentialSpending;
        int sumEX = E + X;
        
        if (sumEX <= 0) {
            return 0;
        }
        
        // 필수 지출 비율 계산
        double p = (double) E / sumEX;
        
        if (p <= 0.8) {
            // 0~0.8 구간: 선형 상승 (0~20점)
            return (int) Math.round(20 * (p / 0.8));
        } else {
            // 0.8 이상이면 만점
            return 20;
        }
    }
    
    /**
     * 절약 지속성 점수 (20점 만점)
     * - 신규 회원: 이번 달 절약률 기반 (10~20점)
     * - 기존 회원: 절약률 변화 기반 (0~20점)
     * 
     * # V4
     * 이번달절약률 = 1 - (이번달소비 / 목표금액)   
     * 지난달절약률 = 1 - (지난달소비 / 목표금액)   # 신규이면 지난달절약률 = 0
     * 
     * 만약 지난달절약률 <= 0:
     *     # 신규 회원
     *     절약점수 = 10 + 10 × 이번달절약률)
     *     # 최대 20점
     * 그렇지 않으면:
     *     # 기존 회원: 절약률 변화 기반
     *     변화 = 이번달절약률 - 지난달절약률
     *     변화_제한 = 최대(-0.3, 최소(0.3, 변화))   # ±30%로 캡
     *     절약점수 = 10 + 10 × (변화_제한 / 0.3)
     */
    private int calculateContinuityScore(Integer currentSpending, Integer currentGoal,
                                         Integer lastMonthSpending, Integer lastMonthGoal) {
        
        if (currentGoal == null || currentGoal == 0) {
            return 0;
        }
        
        // 이번 달 절약률 계산
        double currentSaveRate = 1.0 - ((double) currentSpending / currentGoal);
        
        // 지난 달 절약률 계산
        double lastSaveRate = 0.0;
        if (lastMonthSpending != null && lastMonthGoal != null && lastMonthGoal > 0) {
            lastSaveRate = 1.0 - ((double) lastMonthSpending / lastMonthGoal);
        }
        
        if (lastSaveRate <= 0) {
            // 신규 회원: 10 + 10 × 이번달절약률 (최대 20점)
            int score = (int) Math.round(10 + 10 * currentSaveRate);
            return Math.max(0, Math.min(20, score));
        } else {
            // 기존 회원: 절약률 변화 기반
            double change = currentSaveRate - lastSaveRate;
            
            // ±30%로 캡
            double changeLimit = Math.max(-0.3, Math.min(0.3, change));
            
            // 10 + 10 × (변화_제한 / 0.3) → 0~20점
            int score = (int) Math.round(10 + 10 * (changeLimit / 0.3));
            return Math.max(0, Math.min(20, score));
        }
    }
    
    /**
     * 대시보드 화면용 데이터 조회
     * - 이번달 목표 금액
     * - 이번달 달성률
     * - 소비 점수 (총점)
     * - 두리의 한마디
     * - 카테고리별 소비 TOP 4
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboardData(Long memberId) {
        // 1. 이번 달 목표 조회
        Goal currentGoal = goalRepository.findCurrentMonthGoalByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));
        
        LocalDate startDate = currentGoal.getGoalStartDate();
        LocalDate today = LocalDate.now();
        // 목표 기간의 종료일과 오늘 중 더 이른 날짜까지 계산 (진행 중인 목표의 경우 오늘까지만 계산)
        LocalDate goalEndDate = startDate.plusDays(30);
        LocalDate endDate = today.isBefore(goalEndDate) ? today : goalEndDate;
        Integer goalAmount = currentGoal.getPreviousGoalMoney() != null ? currentGoal.getPreviousGoalMoney() : 0;
        
        // 2. 이번 달 실제 지출 조회 (원 단위)
        int actualSpending = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                memberId, startDate, endDate);
        
        // 3. 달성률 계산 (0~100)
        // 목표 금액은 만원 단위이므로 원 단위로 변환하여 비교
        int goalAmountInWon = goalAmount * 10000;
        int achievementRate = 0;
        if (goalAmountInWon > 0) {
            achievementRate = (int) Math.round((double) actualSpending / goalAmountInWon * 100);
            achievementRate = Math.min(100, Math.max(0, achievementRate)); // 0~100 범위 제한
        }
        
        // 4. Goal 엔티티에서 점수들 조회 (null이면 0으로 설정)
        Integer achievementScore = currentGoal.getGoalAchievementScore() != null 
                ? currentGoal.getGoalAchievementScore() : 0;
        Integer stabilityScore = currentGoal.getGoalStabilityScore() != null 
                ? currentGoal.getGoalStabilityScore() : 0;
        Integer ratioScore = currentGoal.getGoalRatioScore() != null 
                ? currentGoal.getGoalRatioScore() : 0;
        Integer continuityScore = currentGoal.getGoalContinuityScore() != null 
                ? currentGoal.getGoalContinuityScore() : 0;
        
        // 5. 카테고리별 소비 조회 및 TOP 4 추출
        List<Tuple> categorySpendingList = cardHistoryRepository
                .getCategorySpendingByMemberAndDateRange(memberId, startDate, endDate);
        
        // 금액 순으로 정렬하여 TOP 4 추출
        Map<CategoryType, Integer> topCategorySpending = categorySpendingList.stream()
                .map(tuple -> {
                    CategoryType category = tuple.get(0, CategoryType.class);
                    Integer amount = tuple.get(1, Integer.class) != null ? tuple.get(1, Integer.class) : 0;
                    return new AbstractMap.SimpleEntry<>(category, amount);
                })
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 금액 내림차순 정렬
                .limit(4) // TOP 4만 추출
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new // 순서 유지
                ));
        
        return DashboardResponseDto.builder()
                .goalAmount(goalAmount)
                .achievementRate(achievementRate)
                .achievementScore(achievementScore)
                .stabilityScore(stabilityScore)
                .ratioScore(ratioScore)
                .continuityScore(continuityScore)
                .topCategorySpending(topCategorySpending)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ReportResponseDto getReportData(Long memberId) {
        // 1. 이번 달 목표 조회
        Goal currentGoal = goalRepository.findCurrentMonthGoalByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));

        LocalDate startDate = currentGoal.getGoalStartDate();
        LocalDate today = LocalDate.now();
        // 목표 기간의 종료일과 오늘 중 더 이른 날짜까지 계산 (진행 중인 목표의 경우 오늘까지만 계산)
        LocalDate goalEndDate = startDate.plusDays(30);
        LocalDate endDate = today.isBefore(goalEndDate) ? today : goalEndDate;
        Integer goalAmount = currentGoal.getPreviousGoalMoney() != null ? currentGoal.getPreviousGoalMoney() : 0;

        // 2. 이번 달 실제 지출 조회 (원 단위)
        int actualSpending = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                memberId, startDate, endDate);

        // 4. Goal 엔티티에서 점수들 조회 후 총점수 계산 (null이면 0으로 설정)
        Integer achievementScore = currentGoal.getGoalAchievementScore() != null
                ? currentGoal.getGoalAchievementScore() : 0;
        Integer stabilityScore = currentGoal.getGoalStabilityScore() != null
                ? currentGoal.getGoalStabilityScore() : 0;
        Integer ratioScore = currentGoal.getGoalRatioScore() != null
                ? currentGoal.getGoalRatioScore() : 0;
        Integer continuityScore = currentGoal.getGoalContinuityScore() != null
                ? currentGoal.getGoalContinuityScore() : 0;

        Integer goalScore = achievementScore + stabilityScore + ratioScore + continuityScore;

        // 5. 카테고리별 소비 조회
        List<Tuple> categorySpendingList = cardHistoryRepository
                .getAllCategorySpendingByMemberAndDateRange(memberId, startDate, endDate);

        // 금액 순으로 정렬 후 전체 추출
        Map<CategoryType, Integer> CategorySpending = categorySpendingList.stream()
                .map(tuple -> {
                    CategoryType category = tuple.get(0, CategoryType.class);
                    Integer amount = tuple.get(1, Integer.class) != null ? tuple.get(1, Integer.class) : 0;
                    return new AbstractMap.SimpleEntry<>(category, amount);
                })
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 금액 내림차순 정렬
                .limit(12)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new // 순서 유지
                ));

        return ReportResponseDto.builder()
                .goalAmount(goalAmount)
                .actualSpending(actualSpending)
                .goalScore(goalScore)
                .CategorySpending(CategorySpending)
                .build();
    }
    
    /**
     * 과거 목표 데이터 조회 (특정 년/월)
     * - 해당 월의 목표 금액
     * - 해당 월의 달성률
     * - 소비 점수들
     * - 두리의 한마디
     * - 카테고리별 소비 TOP 4
     */
    @Override
    @Transactional(readOnly = true)
    public DashboardResponseDto getPastGoalData(Long memberId, int year, int month) {
        // 1. 특정 년/월의 목표 조회
        LocalDate targetDate = LocalDate.of(year, month, 1);
        Goal pastGoal = goalRepository.findGoalByMemberIdAndStartDate(memberId, targetDate)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_ISNULL));

        LocalDate startDate = pastGoal.getGoalStartDate();
        // 해당 월의 마지막 날까지 계산
        LocalDate endDate = YearMonth.from(startDate).atEndOfMonth();
        Integer goalAmount = pastGoal.getPreviousGoalMoney() != null ? pastGoal.getPreviousGoalMoney() : 0;

        // 2. 해당 월의 실제 지출 조회 (원 단위)
        int actualSpending = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                memberId, startDate, endDate);
        
        // 디버깅 로그 추가
        log.info("과거 목표 데이터 조회 - memberId: {}, year: {}, month: {}", memberId, year, month);
        log.info("목표 시작일: {}, 종료일: {}, 목표 금액(만원): {}", startDate, endDate, goalAmount);
        log.info("실제 지출(원): {}, 목표 금액(원): {}", actualSpending, goalAmount * 10000);

        // 3. 달성률 계산 (0~100)
        // 목표 금액은 만원 단위이므로 원 단위로 변환하여 비교
        int goalAmountInWon = goalAmount * 10000;
        int achievementRate = 0;
        if (goalAmountInWon > 0) {
            achievementRate = (int) Math.round((double) actualSpending / goalAmountInWon * 100);
            achievementRate = Math.min(100, Math.max(0, achievementRate)); // 0~100 범위 제한
            log.info("달성률 계산 결과: {}% (실제 지출: {}원 / 목표 금액: {}원)", achievementRate, actualSpending, goalAmountInWon);
        } else {
            log.warn("목표 금액이 0이므로 달성률을 계산할 수 없습니다.");
        }

        // 4. Goal 엔티티에서 점수들 조회 (null이면 0으로 설정)
        Integer achievementScore = pastGoal.getGoalAchievementScore() != null
                ? pastGoal.getGoalAchievementScore() : 0;
        Integer stabilityScore = pastGoal.getGoalStabilityScore() != null
                ? pastGoal.getGoalStabilityScore() : 0;
        Integer ratioScore = pastGoal.getGoalRatioScore() != null
                ? pastGoal.getGoalRatioScore() : 0;
        Integer continuityScore = pastGoal.getGoalContinuityScore() != null
                ? pastGoal.getGoalContinuityScore() : 0;

        // 5. 카테고리별 소비 조회 및 TOP 4 추출
        List<Tuple> categorySpendingList = cardHistoryRepository
                .getCategorySpendingByMemberAndDateRange(memberId, startDate, endDate);

        // 금액 순으로 정렬하여 TOP 4 추출
        Map<CategoryType, Integer> topCategorySpending = categorySpendingList.stream()
                .map(tuple -> {
                    CategoryType category = tuple.get(0, CategoryType.class);
                    Integer amount = tuple.get(1, Integer.class) != null ? tuple.get(1, Integer.class) : 0;
                    return new AbstractMap.SimpleEntry<>(category, amount);
                })
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue())) // 금액 내림차순 정렬
                .limit(4) // TOP 4만 추출
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new // 순서 유지
                ));

        return DashboardResponseDto.builder()
                .goalAmount(goalAmount)
                .achievementRate(achievementRate)
                .achievementScore(achievementScore)
                .stabilityScore(stabilityScore)
                .ratioScore(ratioScore)
                .continuityScore(continuityScore)
                .topCategorySpending(topCategorySpending)
                .build();
    }

    @Override
    public List<GetGoalDto> getGoalHistory(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Goal> goals = goalRepository.findPastGoalsByMember(memberId);

        if (goals == null || goals.isEmpty()) {
            throw new CustomException(ErrorCode.GOAL_ISNULL);
        }

        return goals.stream()
                .map(GetGoalDto::fromEntity)  // 각 Goal을 GetGoalDto로 변환
                .toList();

    }

}
package com.app.wooridooribe.service.payment;

import com.app.dooribankbe.domain.entity.AccountHistory;
import com.app.dooribankbe.domain.repository.AccountHistoryRepository;
import com.app.wooridooribe.service.sse.SseService;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.Goal;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.entity.type.CategoryType;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.goal.GoalRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.memberCard.MemberCardRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentSyncService {

    private final AccountHistoryRepository accountHistoryRepository;
    private final CardHistoryRepository cardHistoryRepository;
    private final MemberRepository memberRepository;
    private final MemberCardRepository memberCardRepository;
    private final GoalRepository goalRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * db2에서 AccountHistory를 조회하고, db1의 CardHistory에 저장합니다.
     * db1 트랜잭션을 사용하여 CardHistory 저장을 보장합니다.
     */
    @Transactional(transactionManager = "db1TransactionManager", propagation = Propagation.REQUIRED)
    @SuppressWarnings("ConstantConditions")
    public Long syncPayment(String accountNumber, Long historyId) {
        Objects.requireNonNull(accountNumber, "accountNumber must not be null");
        Objects.requireNonNull(historyId, "historyId must not be null");
        log.info("결제 동기화 시작: accountNumber={}, historyId={}", accountNumber, historyId);

        // db2에서 AccountHistory 조회 및 필요한 데이터 추출 (db2 트랜잭션 사용)
        AccountHistory accountHistory = findAccountHistoryFromDb2(historyId, accountNumber);
        
        // db2 트랜잭션 내에서 필요한 모든 데이터를 추출 (트랜잭션 종료 전에)
        String actualAccountNumber = accountHistory.getAccount().getAccountNumber();
        String memberName = accountHistory.getAccount().getDB2Member().getName();
        String phone = accountHistory.getAccount().getDB2Member().getPhone();
        String memberRegistNum = accountHistory.getAccount().getDB2Member().getMemberRegistNum();
        String historyName = accountHistory.getHistoryName();
        Long historyPrice = accountHistory.getHistoryPrice();
        String historyCategoryName = accountHistory.getHistoryCategory().name();
        java.time.LocalDateTime historyDate = accountHistory.getHistoryDate();
        
        // member_regist_num이 7자리면 파싱 (앞 6자리: birth_date, 마지막 1자리: birth_back)
        String birthDate = null;
        String birthBack = null;
        if (memberRegistNum != null && memberRegistNum.length() == 7) {
            birthDate = memberRegistNum.substring(0, 6);
            birthBack = memberRegistNum.substring(6, 7);
            log.info("member_regist_num 파싱: memberRegistNum={}, birthDate={}, birthBack={}", 
                    memberRegistNum, birthDate, birthBack);
        }
        
        log.info("AccountHistory 조회 성공: historyId={}, historyName={}, historyPrice={}, historyCategory={}, historyStatus={}", 
                historyId, historyName, historyPrice, 
                accountHistory.getHistoryCategory(), accountHistory.getHistoryStatus());
        
        if (!actualAccountNumber.equals(accountNumber)) {
            log.error("계좌번호 불일치: 요청 accountNumber={}, 실제 accountNumber={}", 
                    accountNumber, actualAccountNumber);
            throw new CustomException(ErrorCode.BANK_ACCOUNT_MISMATCH);
        }

        // db1에서 Member와 MemberCard 조회 (이름, 주민번호(앞+뒤), 전화번호로 대조)
        Member bankMember = null;
        if (birthDate != null && birthBack != null) {
            log.info("db1에서 Member 조회 시작 (주민번호 대조): memberName={}, birthDate={}, birthBack={}, phone={}", 
                    memberName, birthDate, birthBack, phone);
            bankMember = findWooriDooriMemberByRegistNum(memberName, birthDate, birthBack, phone);
        } else {
            log.warn("주민번호 정보가 없어서 이름과 전화번호로만 조회: memberName={}, phone={}", memberName, phone);
            bankMember = findWooriDooriMemberOptional(memberName, phone);
        }
        
        MemberCard memberCard = null;
        if (bankMember != null) {
            log.info("db1에서 Member 조회 성공: memberId={}, memberName={}", bankMember.getId(), bankMember.getMemberName());
            log.info("db1에서 MemberCard 조회 시작: memberId={}", bankMember.getId());
            memberCard = findMemberCardOptional(bankMember);
            if (memberCard != null) {
                log.info("db1에서 MemberCard 조회 성공: memberCardId={}, cardNum={}", memberCard.getId(), memberCard.getCardNum());
            } else {
                log.warn("db1에서 MemberCard를 찾을 수 없음: memberId={}, memberCard를 null로 설정", bankMember.getId());
            }
        } else {
            log.warn("db1에서 Member를 찾을 수 없음: memberName={}, birthDate={}, birthBack={}, phone={}, memberCard를 null로 설정", 
                    memberName, birthDate, birthBack, phone);
        }

        CategoryType categoryType = convertCategory(historyCategoryName);

        // db1의 CardHistory에 저장 (Member를 찾지 못하면 memberCard는 null)
        CardHistory cardHistory = CardHistory.builder()
                .memberCard(memberCard)
                .historyDate(historyDate.toLocalDate())
                .historyName(historyName)
                .historyPrice(historyPrice.intValue())
                .historyStatus(StatusType.ABLE)
                .historyCategory(categoryType)
                .historyIncludeTotal("Y")
                .historyDutchpay(1)
                .build();

        log.info("CardHistory 저장 시작: historyName={}, historyPrice={}, memberCardId={}, historyDate={}", 
                cardHistory.getHistoryName(), cardHistory.getHistoryPrice(), 
                memberCard != null ? memberCard.getId() : "null", cardHistory.getHistoryDate());
        
        // db1의 tbl_card_history에 저장
        CardHistory saved;
        Long savedId;
        try {
            log.info("CardHistory 저장 시도: historyName={}, historyPrice={}, memberCardId={}, historyDate={}", 
                    cardHistory.getHistoryName(), cardHistory.getHistoryPrice(), 
                    cardHistory.getMemberCard() != null ? cardHistory.getMemberCard().getId() : "null", 
                    cardHistory.getHistoryDate());
            saved = cardHistoryRepository.save(cardHistory);
            log.info("CardHistory save() 호출 완료: saved={}, savedId={}", saved != null, saved != null ? saved.getId() : null);
            
            cardHistoryRepository.flush(); // 즉시 db1에 반영
            log.info("CardHistory flush() 완료");
            
            if (saved == null || saved.getId() == null) {
                log.error("CardHistory 저장 실패: saved가 null이거나 id가 null입니다. accountNumber={}, historyId={}", accountNumber, historyId);
                throw new CustomException(ErrorCode.BANK_SYNC_FAILED);
            }
            
            savedId = saved.getId();
            log.info("CardHistory 저장된 ID: savedId={}", savedId);
            
            // 저장 확인: 실제로 DB에 저장되었는지 확인
            Optional<CardHistory> verifySaved = cardHistoryRepository.findById(savedId);
            if (verifySaved.isEmpty()) {
                log.error("CardHistory 저장 검증 실패: DB에서 조회되지 않음. cardHistoryId={}, accountNumber={}, historyId={}", 
                        savedId, accountNumber, historyId);
                throw new CustomException(ErrorCode.BANK_SYNC_FAILED);
            }
            log.info("CardHistory 저장 검증 성공: cardHistoryId={}", savedId);
        } catch (CustomException e) {
            log.error("CardHistory 저장 중 CustomException 발생: accountNumber={}, historyId={}, error={}", 
                    accountNumber, historyId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("CardHistory 저장 중 예외 발생: accountNumber={}, historyId={}, error={}", 
                    accountNumber, historyId, e.getMessage(), e);
            throw new CustomException(ErrorCode.BANK_SYNC_FAILED);
        }
        
        log.info("결제 동기화 완료: db1의 tbl_card_history 저장 성공 - cardHistoryId={}, accountNumber={}, historyId={}, historyName={}, historyPrice={}, memberCardId={}", 
                savedId, accountNumber, historyId, saved.getHistoryName(), saved.getHistoryPrice(), 
                saved.getMemberCard() != null ? saved.getMemberCard().getId() : "null");
        
        // 위험도 체크 및 알림 전송 (히스토리 추가 시에만, Member가 있는 경우에만)
        if (bankMember != null) {
            checkAndSendRiskNotification(bankMember.getId(), saved.getHistoryPrice());
        } else {
            log.debug("Member가 없어서 위험도 체크 스킵: accountNumber={}, historyId={}", accountNumber, historyId);
        }
        
        return savedId;
    }

    /**
     * 목표 달성률을 계산하고 20% 단위로 위험도가 변경되었는지 체크하여 알림을 전송합니다.
     * 히스토리가 추가될 때만 호출되며, 이전 지출 금액과 비교하여 레벨이 올라갔을 때만 알림을 전송합니다.
     * 기존 MainServiceImpl의 goalPercent 계산 로직을 재사용합니다.
     * 
     * @param memberId 회원 ID
     * @param newHistoryPrice 새로 추가된 히스토리의 금액
     */
    private void checkAndSendRiskNotification(Long memberId, Integer newHistoryPrice) {
        try {
            // 이번 달 목표 조회
            Optional<Goal> goalOpt = goalRepository.findCurrentMonthGoalByMemberId(memberId);
            if (goalOpt.isEmpty()) {
                log.debug("목표가 없어서 위험도 체크 스킵: memberId={}", memberId);
                return;
            }
            
            Goal latestGoal = goalOpt.get();
            LocalDate goalStartDate = latestGoal.getGoalStartDate();
            LocalDate today = LocalDate.now();
            
            // 현재 총 지출 금액 조회 (새로 추가된 히스토리 포함)
            Integer totalPaidMoney = cardHistoryRepository.getTotalSpentByMemberAndDateRange(
                    memberId, goalStartDate, today
            );
            if (totalPaidMoney == null) {
                totalPaidMoney = 0;
            }
            
            // 이전 지출 금액 계산 (새로 추가된 히스토리 제외)
            Integer previousPaidMoney = totalPaidMoney - newHistoryPrice;
            
            // 목표 달성률 계산 (MainServiceImpl 로직 재사용)
            Integer goalMoney = latestGoal.getPreviousGoalMoney();
            if (goalMoney == null || goalMoney <= 0) {
                log.debug("목표 금액이 없어서 위험도 체크 스킵: memberId={}", memberId);
                return;
            }
            
            int goalMoneyInWon = goalMoney * 10000;
            
            // 이전 퍼센트 계산
            int previousPercent = previousPaidMoney > 0 
                    ? (int) Math.round((double) previousPaidMoney / goalMoneyInWon * 100)
                    : 0;
            previousPercent = Math.min(100, Math.max(0, previousPercent));
            
            // 현재 퍼센트 계산
            int currentPercent = (int) Math.round((double) totalPaidMoney / goalMoneyInWon * 100);
            currentPercent = Math.min(100, Math.max(0, currentPercent));
            
            log.info("위험도 체크: memberId={}, previousPercent={}%, currentPercent={}%, previousPaidMoney={}, totalPaidMoney={}, goalMoney={}만원", 
                    memberId, previousPercent, currentPercent, previousPaidMoney, totalPaidMoney, goalMoney);
            
            // 이전 위험도 레벨과 현재 위험도 레벨 계산
            int previousRiskLevel = calculateRiskLevel(previousPercent);
            int currentRiskLevel = calculateRiskLevel(currentPercent);
            
            log.info("위험도 레벨 비교: memberId={}, previousRiskLevel={}, currentRiskLevel={}, previousPercent={}%, currentPercent={}%", 
                    memberId, previousRiskLevel, currentRiskLevel, previousPercent, currentPercent);
            
            // 위험도 레벨이 올라갔을 때만 알림 전송 (20% 단위로)
            if (currentRiskLevel > previousRiskLevel && currentRiskLevel >= 20) {
                sendRiskNotification(memberId, currentPercent, currentRiskLevel);
                log.info("위험도 알림 전송: memberId={}, previousRiskLevel={} -> currentRiskLevel={}", 
                        memberId, previousRiskLevel, currentRiskLevel);
            }
            
        } catch (Exception e) {
            log.error("위험도 체크 중 오류 발생: memberId={}", memberId, e);
        }
    }
    
    /**
     * 현재 퍼센트를 기반으로 위험도 레벨을 계산합니다 (20% 단위).
     * 예: 0-19% -> 0, 20-39% -> 20, 40-59% -> 40, 60-79% -> 60, 80-99% -> 80, 100% 이상 -> 100
     */
    private int calculateRiskLevel(int percent) {
        if (percent >= 100) {
            return 100;
        }
        return (percent / 20) * 20; // 20% 단위로 내림
    }
    
    /**
     * 위험도 알림을 전송합니다.
     */
    private void sendRiskNotification(Long memberId, int currentPercent, int riskLevel) {
        try {
            String title;
            String message;
            String icon;
            String type = "warning";
            
            if (riskLevel >= 100) {
                if (currentPercent > 100) {
                    title = "두리가 화났어요!!";
                    message = String.format("목표 금액의 %d%%를 초과했습니다. 더이상의 소비를 지양해주세요.", currentPercent);
                    icon = "doori_angry";
                    type = "alert";
                } else {
                    title = "두리가 화났어요!!";
                    message = "목표 금액의 100%를 달성했습니다. 더이상의 소비를 지양해주세요.";
                    icon = "doori_angry";
                    type = "alert";
                }
            } else if (riskLevel >= 80) {
                title = "두리에게 변화가 생겼어요 👀";
                message = String.format("목표 금액의 %d%%를 초과했습니다. 소비 계획을 다시 확인해주세요.", riskLevel);
                icon = "doori_face3";
            } else if (riskLevel >= 60) {
                title = "두리에게 변화가 생겼어요 👀";
                message = String.format("목표 금액의 %d%%를 초과했습니다. 소비에 유의해주세요.", riskLevel);
                icon = "doori_face3";
            } else if (riskLevel >= 40) {
                title = "두리에게 변화가 생겼어요 👀";
                message = String.format("목표 금액의 %d%%를 초과했습니다. 소비에 유의해주세요.", riskLevel);
                icon = "doori_face3";
            } else {
                title = "두리에게 변화가 생겼어요 👀";
                message = String.format("목표 금액의 %d%%를 초과했습니다. 소비에 유의해주세요.", riskLevel);
                icon = "doori_face3";
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("title", title);
            data.put("message", message);
            data.put("type", type);
            data.put("icon", icon);
            data.put("actionUrl", "/goal/achievementHistory");
            data.put("percent", currentPercent);
            
            String jsonData = objectMapper.writeValueAsString(data);
            sseService.sendToUser(memberId, "goal", jsonData);
            
            log.info("위험도 알림 전송: memberId={}, riskLevel={}%, currentPercent={}%", 
                    memberId, riskLevel, currentPercent);
        } catch (Exception e) {
            log.error("위험도 알림 전송 실패: memberId={}, riskLevel={}%", memberId, riskLevel, e);
        }
    }

    /**
     * db1에서 Member를 조회합니다 (이름, 주민번호 앞 6자리, 뒷자리 1자리, 전화번호로 대조).
     * 찾지 못하면 null을 반환합니다.
     */
    private Member findWooriDooriMemberByRegistNum(String memberName, String birthDate, String birthBack, String phone) {
        return memberRepository.findByMemberNameAndBirthDateAndBirthBackAndPhone(memberName, birthDate, birthBack, phone)
                .orElse(null);
    }

    /**
     * db1에서 Member를 조회합니다 (이름, 전화번호로만 조회).
     * 찾지 못하면 null을 반환합니다.
     */
    private Member findWooriDooriMemberOptional(String memberName, String phone) {
        return memberRepository.findByMemberNameAndPhone(memberName, phone)
                .orElse(null);
    }

    /**
     * db1에서 MemberCard를 조회합니다. 찾지 못하면 null을 반환합니다.
     */
    private MemberCard findMemberCardOptional(Member member) {
        if (member == null) {
            return null;
        }
        Optional<MemberCard> card = memberCardRepository.findByMemberId(member.getId())
                .stream()
                .findFirst();
        return card.orElse(null);
    }

    /**
     * db2에서 AccountHistory를 조회합니다.
     * db2 트랜잭션을 사용하여 조회합니다.
     * 트랜잭션 커밋 타이밍 문제로 인해 재시도 로직을 포함합니다.
     */
    @Transactional(transactionManager = "db2TransactionManager", propagation = Propagation.REQUIRES_NEW, readOnly = true)
    protected AccountHistory findAccountHistoryFromDb2(Long historyId, String accountNumber) {
        log.info("db2에서 AccountHistory 조회 시도: historyId={}, accountNumber={}", historyId, accountNumber);
        
        int maxRetries = 3;
        int retryDelayMs = 100; // 100ms 대기
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                // Fetch join을 사용하여 account와 db2Member를 함께 조회
                AccountHistory accountHistory = accountHistoryRepository.findByIdWithAccountAndMember(historyId).orElse(null);
                
                if (accountHistory != null) {
                    // Fetch join으로 이미 로드되었으므로 직접 접근 가능
                    String loadedAccountNumber = accountHistory.getAccount().getAccountNumber();
                    
                    log.info("db2에서 AccountHistory 조회 성공: historyId={}, historyName={}, historyPrice={}, accountNumber={}, attempt={}", 
                            historyId, accountHistory.getHistoryName(), accountHistory.getHistoryPrice(), 
                            loadedAccountNumber, attempt);
                    return accountHistory;
                }
                
                // 마지막 시도가 아니면 대기 후 재시도
                if (attempt < maxRetries) {
                    log.warn("db2에서 AccountHistory를 찾을 수 없음 (트랜잭션 커밋 대기 중일 수 있음): historyId={}, attempt={}/{}, {}ms 후 재시도", 
                            historyId, attempt, maxRetries, retryDelayMs);
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    // 재시도 시 지연 시간 증가
                    retryDelayMs *= 2;
                }
            } catch (Exception e) {
                log.error("db2에서 AccountHistory 조회 중 오류 발생: historyId={}, attempt={}", historyId, attempt, e);
                if (attempt == maxRetries) {
                    throw e;
                }
            }
        }
        
        // 모든 재시도 실패
        log.error("db2에서 AccountHistory를 찾을 수 없습니다: historyId={}, accountNumber={}. {}번 시도 후 실패.", 
                historyId, accountNumber, maxRetries);
        throw new CustomException(ErrorCode.BANK_HISTORY_NOT_FOUND);
    }

    private CategoryType convertCategory(String categoryName) {
        try {
            return CategoryType.valueOf(categoryName);
        } catch (IllegalArgumentException e) {
            log.warn("카테고리 변환 실패: {}, 기본값 ETC 사용", categoryName);
            return CategoryType.ETC;
        }
    }
}


package com.app.wooridooribe.service;

import com.app.dooribankbe.domain.entity.AccountHistory;
import com.app.dooribankbe.domain.entity.DB2Member;
import com.app.dooribankbe.domain.entity.HistoryCategory;
import com.app.dooribankbe.domain.entity.MemberAccount;
import com.app.dooribankbe.domain.entity.TransactionType;
import com.app.dooribankbe.domain.repository.AccountHistoryRepository;
import com.app.dooribankbe.domain.repository.DB2MemberRepository;
import com.app.dooribankbe.domain.repository.MemberAccountRepository;
import com.app.wooridooribe.entity.CardHistory;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.entity.MemberCard;
import com.app.wooridooribe.entity.type.StatusType;
import com.app.wooridooribe.repository.cardHistory.CardHistoryQueryDslImplTest;
import com.app.wooridooribe.repository.cardHistory.CardHistoryRepository;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.repository.memberCard.MemberCardRepository;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.JDBCConnectionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("DB2 멤버 동기화 테스트")
@Slf4j
class Db2MemberSyncTest {
    private static final Logger logger = LoggerFactory.getLogger(CardHistoryQueryDslImplTest.class);

    @BeforeAll
    static void loadEnv() {
        try {
            Dotenv dotenv = Dotenv.load();
            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );
            logger.info(".env 파일 로드 완료");
        } catch (Exception e) {
            logger.warn(".env 파일을 찾을 수 없습니다. 환경 변수를 확인하세요: {}", e.getMessage());
        }
    }

    @Autowired
    private MemberRepository wooriDooriMemberRepository; // DB1 (우리두리)

    @Autowired
    private DB2MemberRepository dooriBankMemberRepository; // DB2 (DooriBank)

    @Test
    @DisplayName("1번 DB의 모든 유저를 2번 DB에 동기화")
    @Transactional(transactionManager = "db2TransactionManager")
    @Commit // 트랜잭션 커밋 (롤백 방지)
    void syncAllMembersFromDb1ToDb2() {
        // 1. DB1에서 모든 멤버 조회
        List<Member> db1Members = wooriDooriMemberRepository.findAll();
        
        System.out.println("DB1에서 조회된 멤버 수: " + db1Members.size());
        
        int successCount = 0;
        int skipCount = 0;
        
        // 2. 각 멤버를 DB2에 저장
        for (Member db1Member : db1Members) {
            try {
                // 필수 필드 검증
                if (db1Member.getMemberName() == null || db1Member.getPhone() == null) {
                    System.out.println("스킵: ID=" + db1Member.getId() + " (name 또는 phone이 null)");
                    skipCount++;
                    continue;
                }
                
                // 이미 존재하는지 확인 (name과 phone으로)
                boolean exists = dooriBankMemberRepository.findByNameAndPhone(
                    db1Member.getMemberName(), 
                    db1Member.getPhone()
                ).isPresent();
                
                if (exists) {
                    System.out.println("스킵: 이미 존재하는 멤버 - name=" + db1Member.getMemberName() + ", phone=" + db1Member.getPhone());
                    skipCount++;
                    continue;
                }
                
                // DB2 Member 엔티티 생성
                DB2Member db2Member = DB2Member.builder()
                        .name(db1Member.getMemberName())
                    .phone(db1Member.getPhone())
                    .build();
                
                // DB2에 저장
                DB2Member saved = dooriBankMemberRepository.save(db2Member);
                
                System.out.println("저장 성공: ID=" + saved.getId() + 
                    ", name=" + saved.getName() + 
                    ", phone=" + saved.getPhone());
                successCount++;
                
            } catch (Exception e) {
                System.err.println("저장 실패: ID=" + db1Member.getId() + 
                    ", name=" + db1Member.getMemberName() + 
                    ", error=" + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== 동기화 완료 ===");
        System.out.println("전체 멤버 수: " + db1Members.size());
        System.out.println("성공: " + successCount);
        System.out.println("스킵: " + skipCount);
        
        assertTrue(successCount > 0 || skipCount > 0, "최소한 하나의 멤버는 처리되어야 합니다");
    }

    @Autowired
    private MemberCardRepository memberCardRepository; // DB1 (우리두리)

    @Autowired
    private MemberAccountRepository memberAccountRepository; // DB2 (DooriBank)

    @Autowired
    @Qualifier("db2TransactionManager")
    private org.springframework.transaction.PlatformTransactionManager db2TransactionManager;

    @Autowired
    private CardHistoryRepository cardHistoryRepository; // DB1 (우리두리)

    @Autowired
    private AccountHistoryRepository accountHistoryRepository; // DB2 (DooriBank)

    @Test
    @DisplayName("1번 DB의 모든 카드를 2번 DB에 memberAccount로 동기화 (멀티스레딩)")
    @Transactional(transactionManager = "db2TransactionManager")
    @Commit // 트랜잭션 커밋 (롤백 방지)
    void syncAllMemberCardsFromDb1ToDb2() {
        long startTime = System.currentTimeMillis();
        
        // 1. DB1에서 모든 MemberCard 조회
        List<MemberCard> db1MemberCards = memberCardRepository.findAll();

        System.out.println("DB1에서 조회된 카드 수: " + db1MemberCards.size());
        
        // 동시성 안전을 위한 AtomicInteger
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);
        
        // 스레드 풀 생성 (연결 풀 크기에 맞춰 조절, 최대 10개로 제한하여 DB 연결 타임아웃 방지)
        int threadPoolSize = Math.min(10, Math.max(4, Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        
        System.out.println("멀티스레딩 시작: 스레드 풀 크기 = " + threadPoolSize);
        
        // TransactionTemplate 생성 (각 작업마다 별도 트랜잭션)
        TransactionTemplate transactionTemplate = new TransactionTemplate(db2TransactionManager);
        
        // 2. 각 카드를 병렬로 처리
        List<CompletableFuture<Void>> futures = db1MemberCards.stream()
                .map(db1Card -> CompletableFuture.runAsync(() -> {
                    processCardSync(db1Card, successCount, skipCount, transactionTemplate);
                }, executor))
                .toList();
        
        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        executor.shutdown();
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("\n=== 카드 동기화 완료 ===");
        System.out.println("전체 카드 수: " + db1MemberCards.size());
        System.out.println("성공: " + successCount.get());
        System.out.println("스킵: " + skipCount.get());
        System.out.println("소요 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");
        
        assertTrue(successCount.get() > 0 || skipCount.get() > 0, "최소한 하나의 카드는 처리되어야 합니다");
    }
    
    /**
     * 개별 카드 동기화 처리 (각 작업은 별도 트랜잭션으로 처리)
     */
    private void processCardSync(MemberCard db1Card, AtomicInteger successCount, AtomicInteger skipCount, TransactionTemplate transactionTemplate) {
        transactionTemplate.executeWithoutResult(status -> {
            try {
                // PK 동기화를 위한 ID 검증 (계정계/채널계 동기화)
                if (db1Card.getId() == null) {
                    skipCount.incrementAndGet();
                    return;
                }
                
                // 필수 필드 검증
                if (db1Card.getCardNum() == null || db1Card.getCardPw() == null ||
                        db1Card.getCardUserName() == null || db1Card.getCardUserRegistNum() == null ||
                        db1Card.getCardUserRegistBack() == null) {
                    skipCount.incrementAndGet();
                    return;
                }
                
                // 이미 존재하는지 확인 (ID로) - 계정계 PK와 채널계 PK 동기화 확인
                boolean existsById = memberAccountRepository.findById(db1Card.getId()).isPresent();
                if (existsById) {
                    skipCount.incrementAndGet();
                    return;
                }
                
                // account_num 중복 체크 (멀티스레딩 환경에서 동시성 문제 방지)
                boolean existsByAccountNum = memberAccountRepository.findByAccountNumber(db1Card.getCardNum()).isPresent();
                if (existsByAccountNum) {
                    skipCount.incrementAndGet();
                    return;
                }
                
                // 카드 소유자 정보를 기반으로 DB2의 Member 찾기
                String ownerRegist = db1Card.getCardUserRegistNum() + db1Card.getCardUserRegistBack();
                DB2Member db2Member = dooriBankMemberRepository.findByMemberRegistNum(ownerRegist)
                        .orElse(null);
                
                if (db2Member == null) {
                    skipCount.incrementAndGet();
                    return;
                }
                
                // DB2 MemberAccount 엔티티 생성
                // 계정계(DB1) PK → 채널계(DB2) PK 동기화 (동기화/추적 목적)
                MemberAccount db2Account = MemberAccount.builder()
                    .id(db1Card.getId()) // 계정계 PK = 채널계 PK (동기화)
                    .DB2Member(db2Member)
                    .accountNumber(db1Card.getCardNum())
                    .accountPassword(db1Card.getCardPw())
                    .accountCreateAt(db1Card.getCardCreateAt() != null ? db1Card.getCardCreateAt() : java.time.LocalDate.now())
                    .balance(0L) // 기본값
                    .build();
                
                // DB2에 저장
                memberAccountRepository.save(db2Account);
                
                successCount.incrementAndGet();
                
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // 중복 키 에러는 멀티스레딩 환경에서 정상적인 경우 (다른 스레드가 이미 저장함)
                if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                    skipCount.incrementAndGet();
                    // 롤백하지 않고 스킵만 처리
                } else {
                    System.err.println("저장 실패 (제약조건 위반): ID=" + db1Card.getId() + 
                        ", cardNum=" + db1Card.getCardNum() + 
                        ", error=" + e.getMessage());
                    skipCount.incrementAndGet();
                    status.setRollbackOnly();
                }
            } catch (Exception e) {
                System.err.println("저장 실패: ID=" + db1Card.getId() + 
                    ", cardNum=" + db1Card.getCardNum() + 
                    ", error=" + e.getMessage());
                skipCount.incrementAndGet();
                status.setRollbackOnly(); // 에러 발생 시 롤백
            }
        });
    }

    @Test
    @DisplayName("DB1의 모든 카드 히스토리를 DB2의 account_history로 동기화 (멀티스레딩)")
    @Transactional(transactionManager = "db2TransactionManager")
    @Commit // 트랜잭션 커밋 (롤백 방지)
    void syncAllCardHistoriesFromDb1ToDb2() {
        long startTime = System.currentTimeMillis();

        // 1. DB1에서 모든 CardHistory 조회
        List<CardHistory> db1CardHistories = cardHistoryRepository.findAll();

        System.out.println("DB1에서 조회된 카드 히스토리 수: " + db1CardHistories.size());

        // 동시성 안전을 위한 AtomicInteger
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // 스레드 풀 생성 (연결 풀 크기에 맞춰 조절, 최대 10개로 제한하여 DB 연결 타임아웃 방지)
        int threadPoolSize = Math.min(10, Math.max(4, Runtime.getRuntime().availableProcessors()));
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);

        System.out.println("멀티스레딩 시작: 스레드 풀 크기 = " + threadPoolSize);

        // TransactionTemplate 생성 (각 작업마다 별도 트랜잭션)
        TransactionTemplate transactionTemplate = new TransactionTemplate(db2TransactionManager);

        // 2. 각 히스토리를 병렬로 처리
        List<CompletableFuture<Void>> futures = db1CardHistories.stream()
                .map(db1History -> CompletableFuture.runAsync(() -> {
                    processHistorySync(db1History, successCount, skipCount, transactionTemplate);
                }, executor))
                .toList();

        // 모든 작업 완료 대기
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        executor.shutdown();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("\n=== 카드 히스토리 동기화 완료 ===");
        System.out.println("전체 히스토리 수: " + db1CardHistories.size());
        System.out.println("성공: " + successCount.get());
        System.out.println("스킵: " + skipCount.get());
        System.out.println("소요 시간: " + duration + "ms (" + (duration / 1000.0) + "초)");

        assertTrue(successCount.get() > 0 || skipCount.get() > 0, "최소한 하나의 히스토리는 처리되어야 합니다");
    }

    /**
     * 개별 카드 히스토리 동기화 처리 (각 작업은 별도 트랜잭션으로 처리, 재시도 로직 포함)
     */
    private void processHistorySync(CardHistory db1History, AtomicInteger successCount, AtomicInteger skipCount, TransactionTemplate transactionTemplate) {
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (retryCount < maxRetries && !success) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    try {
                        processHistorySyncInternal(db1History, successCount, skipCount);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 중복 키 에러는 재시도하지 않음
                        if (e.getMessage() != null && e.getMessage().contains("Duplicate entry")) {
                            skipCount.incrementAndGet();
                        } else {
                            throw e;
                        }
                    }
                });
                success = true;
            } catch (RuntimeException e) {
                // CommunicationsException이나 SocketTimeoutException은 RuntimeException으로 래핑될 수 있음
                Throwable cause = e.getCause();
                boolean isConnectionError = e.getMessage() != null && (
                    e.getMessage().contains("Communications link failure") ||
                    e.getMessage().contains("SocketTimeoutException") ||
                    e.getMessage().contains("Read timed out")
                ) || (cause != null && (
                    cause.getClass().getName().contains("CommunicationsException") ||
                    cause.getClass().getName().contains("SocketTimeoutException")
                ));
                
                if (isConnectionError) {
                    retryCount++;
                    if (retryCount < maxRetries) {
                        try {
                            Thread.sleep(1000 * retryCount); // 지수 백오프: 1초, 2초, 3초
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            skipCount.incrementAndGet();
                            return;
                        }
                    } else {
                        System.err.println("저장 실패 (재시도 초과): ID=" + db1History.getId() +
                                ", error=" + e.getMessage());
                        skipCount.incrementAndGet();
                    }
                } else {
                    // 연결 에러가 아닌 다른 RuntimeException은 재시도하지 않음
                    System.err.println("저장 실패: ID=" + db1History.getId() +
                            ", error=" + e.getMessage());
                    skipCount.incrementAndGet();
                    break;
                }
            } catch (Exception e) {
                // 다른 예외는 재시도하지 않음
                System.err.println("저장 실패: ID=" + db1History.getId() +
                        ", error=" + e.getMessage());
                skipCount.incrementAndGet();
                break;
            }
        }
    }
    
    /**
     * 실제 히스토리 동기화 로직
     */
    private void processHistorySyncInternal(CardHistory db1History, AtomicInteger successCount, AtomicInteger skipCount) {
        // 필수 필드 검증
        if (db1History.getMemberCard() == null || db1History.getMemberCard().getId() == null) {
            skipCount.incrementAndGet();
            return;
        }

        if (db1History.getHistoryDate() == null || db1History.getHistoryName() == null ||
                db1History.getHistoryPrice() == null || db1History.getHistoryStatus() == null ||
                db1History.getHistoryCategory() == null) {
            skipCount.incrementAndGet();
            return;
        }

        // DB2에서 해당 MemberAccount 찾기 (MemberCard.id = MemberAccount.id로 매핑)
        Long memberCardId = db1History.getMemberCard().getId();
        MemberAccount db2Account = memberAccountRepository.findById(memberCardId).orElse(null);

        if (db2Account == null) {
            skipCount.incrementAndGet();
            return;
        }

        // LocalDate → LocalDateTime 변환
        LocalDate historyDate = db1History.getHistoryDate();
        LocalDateTime historyDateTime = historyDate.atStartOfDay();

        // CategoryType → HistoryCategory 변환 (enum 이름이 동일하므로 valueOf 사용)
        HistoryCategory historyCategory;
        try {
            historyCategory = HistoryCategory.valueOf(db1History.getHistoryCategory().name());
        } catch (IllegalArgumentException e) {
            historyCategory = HistoryCategory.ETC; // 기본값
        }

        // StatusType → TransactionType 변환
        TransactionType transactionType;
        if (db1History.getHistoryStatus() == StatusType.ABLE) {
            transactionType = TransactionType.PAYMENT;
        } else {
            transactionType = TransactionType.TRANSFER_OUT; // UNABLE은 출금으로 처리
        }

        // Integer → Long 변환
        Long historyPrice = Long.valueOf(db1History.getHistoryPrice());

        // DB2 AccountHistory 엔티티 생성
        AccountHistory db2History = AccountHistory.builder()
                .account(db2Account) // FK: MemberAccount (MemberCard.id = MemberAccount.id)
                .historyDate(historyDateTime)
                .historyPrice(historyPrice)
                .historyStatus(transactionType)
                .historyCategory(historyCategory)
                .historyName(db1History.getHistoryName())
                .historyTransferTarget(null) // nullable 필드
                .build();

        // DB2에 저장
        accountHistoryRepository.save(db2History);
        
        successCount.incrementAndGet();
    }

    @Test
    @DisplayName("우리두리 카드 사용자 → 두리뱅킹 회원 동기화")
    @Transactional(transactionManager = "db2TransactionManager")
    @Commit // 트랜잭션 커밋 (롤백 방지)
    void syncCardOwnersToDb2Members() {
        List<MemberCard> memberCards = memberCardRepository.findAll();
        Set<String> processedRegNums = new HashSet<>();
        Random random = new Random();

        int successCount = 0;
        int skipCount = 0;

        for (MemberCard card : memberCards) {
            try {
                String name = card.getCardUserName();
                String registFront = card.getCardUserRegistNum();
                String registBack = card.getCardUserRegistBack();

                if (name == null || registFront == null || registBack == null) {
                    log.warn("스킵: ID={} (이름 또는 주민번호가 null)", card.getId());
                    skipCount++;
                    continue;
                }

                String fullRegist = registFront + registBack;

                // 동일한 주민등록번호는 한 번만 처리
                if (!processedRegNums.add(fullRegist)) {
                    skipCount++;
                    continue;
                }

                boolean exists = dooriBankMemberRepository.findByMemberRegistNum(fullRegist).isPresent();
                if (exists) {
                    log.info("스킵: 이미 존재하는 회원 - name={}, regist={}", name, fullRegist);
                    skipCount++;
                    continue;
                }

                String phone = String.format("010-%04d-%04d", random.nextInt(10000), random.nextInt(10000));

                DB2Member newMember = DB2Member.builder()
                        .name(name)
                        .phone(phone)
                        .build();
                newMember.setMemberRegistNum(fullRegist);

                DB2Member saved = dooriBankMemberRepository.save(newMember);
                log.info("카드 사용자 동기화 성공: id={}, name={}, phone={}", saved.getId(), saved.getName(), saved.getPhone());
                successCount++;
            } catch (Exception e) {
                log.error("카드 사용자 동기화 실패: cardId={}, error={}", card.getId(), e.getMessage(), e);
            }
        }

        log.info("카드 사용자 동기화 완료 - 전체:{}, 성공:{}, 스킵:{}", memberCards.size(), successCount, skipCount);
        assertTrue(successCount > 0 || skipCount > 0, "최소한 하나의 카드 사용자는 처리되어야 합니다");
    }
}

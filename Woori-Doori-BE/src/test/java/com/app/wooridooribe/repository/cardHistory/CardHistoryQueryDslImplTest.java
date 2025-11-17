package com.app.wooridooribe.repository.cardHistory;

import com.app.wooridooribe.entity.type.CategoryType;
import com.querydsl.core.Tuple;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CardHistoryQueryDslImpl.getCategorySpendingByMemberAndDateRange 테스트
 */
@SpringBootTest
@Slf4j
@Transactional
public class CardHistoryQueryDslImplTest {

    private static final Logger logger = LoggerFactory.getLogger(CardHistoryQueryDslImplTest.class);

    @Autowired
    private CardHistoryRepository cardHistoryQueryDsl;

    /**
     * 테스트 실행 전 .env 파일 로드
     */
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

    @Test
    @DisplayName("getCategorySpendingByMemberAndDateRange 테스트")
    void testGetCategorySpendingByMemberAndDateRange() {
        // Given
        Long memberId = 3L; // 실제 DB에 있는 memberId로 변경하세요
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 30);

        log.info("=== getCategorySpendingByMemberAndDateRange 테스트 시작 ===");
        log.info("memberId: {}, startDate: {}, endDate: {}", memberId, startDate, endDate);

        // When
        List<Tuple> result = cardHistoryQueryDsl.getCategorySpendingByMemberAndDateRange(
                memberId, startDate, endDate
        );

        int total = cardHistoryQueryDsl.getTotalSpentByMemberAndDateRange(memberId, startDate, endDate);
        log.info( "총 쓴양 : {}", total);
        // Then
        assertNotNull(result, "결과가 null이 아니어야 합니다.");
        log.info("조회된 카테고리 수: {}", result.size());

        // 결과 출력
        for (int i = 0; i < result.size(); i++) {
            Tuple tuple = result.get(i);
            CategoryType category = tuple.get(0, CategoryType.class);
            Integer totalPrice = tuple.get(1, Integer.class);
            log.info("{}위: 카테고리={}, 총액={}원", i + 1, category, totalPrice);
        }

        // 검증: 결과가 5개 이하여야 함 (limit(5))
        assertTrue(result.size() <= 5, "결과는 최대 5개여야 합니다. 실제: " + result.size());

        // 검증: 금액 내림차순 정렬 확인
        for (int i = 0; i < result.size() - 1; i++) {
            Integer currentPrice = result.get(i).get(1, Integer.class);
            Integer nextPrice = result.get(i + 1).get(1, Integer.class);
            assertTrue(currentPrice >= nextPrice, 
                    String.format("금액이 내림차순으로 정렬되어야 합니다. [%d] >= [%d]", currentPrice, nextPrice));
        }

        // 검증: 각 결과의 카테고리와 금액이 null이 아니어야 함
        for (Tuple tuple : result) {
            CategoryType category = tuple.get(0, CategoryType.class);
            Integer totalPrice = tuple.get(1, Integer.class);
            assertNotNull(category, "카테고리가 null이 아니어야 합니다.");
            assertNotNull(totalPrice, "총액이 null이 아니어야 합니다.");
            assertTrue(totalPrice > 0, "총액이 0보다 커야 합니다.");
        }

        log.info("=== getCategorySpendingByMemberAndDateRange 테스트 완료 ===");
    }
}


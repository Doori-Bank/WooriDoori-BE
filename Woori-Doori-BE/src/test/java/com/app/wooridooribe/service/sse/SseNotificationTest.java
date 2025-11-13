package com.app.wooridooribe.service.sse;

import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.entity.Member;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SSE 알림 전송 테스트
 * 실제 사용자에게 알림을 보내서 테스트할 수 있습니다.
 * 
 * 주의: 실제 DB 연결이 필요합니다. 환경 변수가 설정되어 있어야 합니다.
 */
@SpringBootTest
@Slf4j
@Transactional
public class SseNotificationTest {

    private static final Logger logger = LoggerFactory.getLogger(SseNotificationTest.class);

    @Autowired
    private SseService sseService;

    @Autowired
    private MemberRepository memberRepository;

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

    /**
     * 특정 사용자에게 알림 전송 테스트
     * memberId를 파라미터로 받아서 해당 사용자에게 알림을 보냅니다.
     */
    @Test
    void testSendNotificationToUser() {
        // 테스트할 사용자 ID (실제 DB에 있는 사용자 ID로 변경하세요)
        Long memberId = 3L; // 또는 실제 사용자 ID
        
        // 먼저 사용자가 존재하는지 확인
        Optional<Member> member = memberRepository.findById(memberId);
        if (member.isEmpty()) {
            log.warn("사용자를 찾을 수 없습니다. memberId: {}", memberId);
            log.info("사용 가능한 사용자 ID를 확인하세요.");
            return;
        }

        log.info("=== SSE 알림 전송 테스트 시작 ===");
        log.info("대상 사용자: {} (ID: {})", member.get().getMemberName(), memberId);

        // 1. 단순 문자열 알림
        log.info("1. 단순 문자열 알림 전송...");
        sseService.sendToUser(memberId, "notification", "테스트 알림입니다!");

        // 2. 객체 알림 (JSON으로 자동 변환)
        log.info("2. 객체 알림 전송...");
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", "테스트 알림");
        notificationData.put("message", "이것은 JUnit 테스트로 보낸 알림입니다!");
        notificationData.put("type", "test");
        notificationData.put("timestamp", System.currentTimeMillis());
        sseService.sendToUser(memberId, "notification", notificationData);

        // 3. 점수 업데이트 알림
        log.info("3. 점수 업데이트 알림 전송...");
        Map<String, Object> scoreData = new HashMap<>();
        scoreData.put("totalScore", 85);
        scoreData.put("achievementScore", 90);
        scoreData.put("stabilityScore", 80);
        scoreData.put("message", "목표 점수가 업데이트되었습니다!");
        sseService.sendToUser(memberId, "scoreUpdated", scoreData);

        // 4. 목표 달성 알림
        log.info("4. 목표 달성 알림 전송...");
        Map<String, Object> achievementData = new HashMap<>();
        achievementData.put("goalName", "월간 소비 목표");
        achievementData.put("message", "축하합니다! 목표를 달성했습니다! 🎉");
        achievementData.put("achievedAt", System.currentTimeMillis());
        sseService.sendToUser(memberId, "goalAchieved", achievementData);

        log.info("=== SSE 알림 전송 테스트 완료 ===");
        log.info("프론트엔드에서 알림이 수신되는지 확인하세요!");
        
        // 테스트가 너무 빨리 끝나지 않도록 잠시 대기
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 모든 연결된 사용자에게 브로드캐스트 테스트
     */
    @Test
    void testBroadcastNotification() {
        log.info("=== 브로드캐스트 알림 테스트 시작 ===");

        Map<String, Object> broadcastData = new HashMap<>();
        broadcastData.put("title", "시스템 공지");
        broadcastData.put("message", "이것은 모든 사용자에게 전송되는 브로드캐스트 알림입니다!");
        broadcastData.put("type", "broadcast");
        broadcastData.put("timestamp", System.currentTimeMillis());

        sseService.broadcast("announcement", broadcastData);

        log.info("=== 브로드캐스트 알림 전송 완료 ===");
        log.info("현재 연결된 모든 사용자에게 알림이 전송되었습니다.");

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 사용자 목록 조회 및 알림 전송
     */
    @Test
    void testSendNotificationToAllUsers() {
        log.info("=== 모든 사용자에게 개별 알림 전송 테스트 시작 ===");

        // DB에 있는 모든 사용자 조회
        List<Member> members = memberRepository.findAll();
        log.info("총 사용자 수: {}", members.size());

        for (Member member : members) {
            Map<String, Object> data = new HashMap<>();
            data.put("message", String.format("%s님, 안녕하세요! 테스트 알림입니다.", member.getMemberName()));
            data.put("type", "personal");
            data.put("timestamp", System.currentTimeMillis());

            sseService.sendToUser(member.getId(), "notification", data);
            log.info("알림 전송: {} (ID: {})", member.getMemberName(), member.getId());
        }

        log.info("=== 모든 사용자에게 알림 전송 완료 ===");

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}


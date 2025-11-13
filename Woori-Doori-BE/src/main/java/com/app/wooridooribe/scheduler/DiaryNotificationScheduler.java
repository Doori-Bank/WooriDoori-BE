package com.app.wooridooribe.scheduler;

import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.service.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 일기 작성 알림 스케줄러
 * 매일 오후 3시에 모든 활성 유저에게 일기 작성 알림을 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiaryNotificationScheduler {
    
    private final SseService sseService;
    private final MemberRepository memberRepository;
    
    /**
     * 매일 오후 3시에 실행
     * cron 표현식: 초 분 시 일 월 요일
     * 0 0 15 * * ? = 매일 15:00:00 (오후 3시)
     */
    @Scheduled(fixedRate = 600000) // 5분 = 300000ms
    public void sendDiaryNotification() {
        log.info("=== 일기 작성 알림 스케줄러 시작 ===");
        
        try {
            // 3개월 내 로그인한 활성 유저 조회
            LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
            List<Member> activeMembers = memberRepository.findMembersLoggedInWithinThreeMonths(threeMonthsAgo);
            
            log.info("일기 작성 알림 전송 시작 - 활성 유저 수: {}", activeMembers.size());
            
            // 일기 알림 데이터 생성
            Map<String, Object> diaryNotificationData = new HashMap<>();
            diaryNotificationData.put("title", "소비 일기를 쓸 시간이에요!");
            diaryNotificationData.put("message", "오늘 하루 소비 일기를 쓰러 갈까요?");
            diaryNotificationData.put("icon", "https://cloud5-img-storage.s3.ap-northeast-2.amazonaws.com/doori-icon/diary.png");
            diaryNotificationData.put("type", "diary");
            diaryNotificationData.put("timestamp", System.currentTimeMillis());
            
            int successCount = 0;
            int failCount = 0;
            
            // 모든 활성 유저에게 알림 전송
            for (Member member : activeMembers) {
                try {
                    sseService.sendToUser(member.getId(), "notification", diaryNotificationData);
                    successCount++;
                    log.debug("일기 알림 전송 성공 - memberId: {}", member.getId());
                } catch (Exception e) {
                    failCount++;
                    log.warn("일기 알림 전송 실패 - memberId: {}", member.getId(), e);
                }
            }
            
            log.info("=== 일기 작성 알림 전송 완료 - 성공: {}, 실패: {} ===", successCount, failCount);
        } catch (Exception e) {
            log.error("=== 일기 작성 알림 스케줄러 중 에러 발생 ===", e);
        }
    }
    
    /**
     * 테스트용: 매 5분마다 실행 (개발 환경에서만 사용)
     * 운영 환경에서는 주석 처리하거나 삭제
     */
    // @Scheduled(fixedRate = 300000) // 5분 = 300000ms
    // public void sendDiaryNotificationTest() {
    //     log.info("=== [테스트] 일기 작성 알림 스케줄러 시작 ===");
    //     try {
    //         LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);
    //         List<Member> activeMembers = memberRepository.findMembersLoggedInWithinThreeMonths(threeMonthsAgo);
    //         
    //         Map<String, Object> diaryNotificationData = new HashMap<>();
    //         diaryNotificationData.put("title", "소비 일기를 쓸 시간이에요!");
    //         diaryNotificationData.put("message", "오늘 하루 소비 일기를 쓰러 갈까요?");
    //         diaryNotificationData.put("icon", "https://cloud5-img-storage.s3.ap-northeast-2.amazonaws.com/doori-icon/diary.svg");
    //         diaryNotificationData.put("type", "diary");
    //         
    //         for (Member member : activeMembers) {
    //             try {
    //                 sseService.sendToUser(member.getId(), "notification", diaryNotificationData);
    //                 log.info("[테스트] 일기 알림 전송 - memberId: {}", member.getId());
    //             } catch (Exception e) {
    //                 log.warn("[테스트] 일기 알림 전송 실패 - memberId: {}", member.getId(), e);
    //             }
    //         }
    //         log.info("=== [테스트] 일기 작성 알림 전송 완료 ===");
    //     } catch (Exception e) {
    //         log.error("=== [테스트] 일기 작성 알림 스케줄러 중 에러 발생 ===", e);
    //     }
    // }
}


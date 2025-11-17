package com.app.wooridooribe.scheduler;

import com.app.wooridooribe.controller.SseController;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.repository.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 소비 일기(DIARY) 알림 스케줄러
 * - 최근 6개월 내 로그인한 회원들에게 매일 UTC 기준 22시에 일기 작성 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DiaryNotificationScheduler {

    private final MemberRepository memberRepository;
    private final SseController sseController;

    /**
     * 매일 UTC 기준 22:00에 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 22 * * *", zone = "UTC")
    public void sendDailyDiaryNotifications() {
        LocalDateTime threeMonthsAgo = LocalDateTime.now().minusMonths(3);

        List<Member> activeMembers = memberRepository.findMembersLoggedInWithinThreeMonths(threeMonthsAgo);
        log.info("DIARY 알림 스케줄 실행 - 최근 3개월 내 로그인한 회원 수: {}", activeMembers.size());

        int successCount = 0;
        int failCount = 0;

        for (Member member : activeMembers) {
            try {
                sseController.sendDiaryNotification(member.getId());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("DIARY 알림 전송 실패 - memberId: {}", member.getId(), e);
            }
        }

        log.info("DIARY 알림 스케줄 완료 - 성공: {}, 실패: {}", successCount, failCount);
    }
}



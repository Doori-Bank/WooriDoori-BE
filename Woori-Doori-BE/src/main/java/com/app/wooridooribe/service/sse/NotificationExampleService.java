package com.app.wooridooribe.service.sse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * SSE 알림 전송 예제 서비스
 * 실제 서비스에서 SseService를 주입받아 사용하는 방법을 보여줍니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationExampleService {
    
    private final SseService sseService;
    
    /**
     * 특정 사용자에게 알림 전송 예제
     * @param memberId 사용자 ID
     * @param message 알림 메시지
     */
    public void sendNotificationToUser(Long memberId, String message) {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("message", message);
        notificationData.put("type", "notification");
        notificationData.put("timestamp", System.currentTimeMillis());
        notificationData.put("icon", "/favicon.ico"); // 아이콘 추가
        
        sseService.sendToUser(memberId, "notification", notificationData);
    }
    
    /**
     * 점수 업데이트 알림 전송 예제
     * @param memberId 사용자 ID
     * @param score 점수 데이터
     */
    public void sendScoreUpdate(Long memberId, Object score) {
        sseService.sendToUser(memberId, "scoreUpdated", score);
    }
    
    /**
     * 모든 사용자에게 브로드캐스트 예제
     * @param message 공지 메시지
     */
    public void broadcastToAll(String message) {
        Map<String, Object> broadcastData = new HashMap<>();
        broadcastData.put("message", message);
        broadcastData.put("type", "broadcast");
        broadcastData.put("timestamp", System.currentTimeMillis());
        broadcastData.put("icon", "/favicon.ico"); // 아이콘 추가
        
        sseService.broadcast("announcement", broadcastData);
    }
}


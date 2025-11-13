package com.app.wooridooribe.service.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SseService {
    
    // 각 사용자별 SseEmitter 저장 (memberId -> SseEmitter)
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    
    // 타임아웃 설정 (30분)
    private static final long TIMEOUT = 30 * 60 * 1000L;
    
    // 하트비트 주기 (30초마다 ping 전송)
    private static final long HEARTBEAT_INTERVAL = 30;
    
    // 하트비트를 관리할 스케줄러
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    /**
     * SSE 연결 생성
     * @param memberId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long memberId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT);
        
        // 완료/타임아웃/에러 시 제거
        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료 - memberId: {}", memberId);
            emitters.remove(memberId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 타임아웃 - memberId: {}", memberId);
            emitters.remove(memberId);
        });
        
        emitter.onError((ex) -> {
            log.error("SSE 에러 - memberId: {}, error: {}", memberId, ex.getMessage());
            emitters.remove(memberId);
        });
        
        emitters.put(memberId, emitter);
        log.info("SSE 연결 생성 - memberId: {}, 현재 연결 수: {}", memberId, emitters.size());
        
        // 연결 확인 메시지 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결이 성공적으로 설정되었습니다."));
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패 - memberId: {}", memberId, e);
            emitters.remove(memberId);
            return emitter;
        }
        
        // 하트비트 시작 (백그라운드에서도 연결 유지)
        startHeartbeat(memberId, emitter);
        
        return emitter;
    }
    
    /**
     * 하트비트 시작 (백그라운드 연결 유지)
     * 브라우저가 백그라운드에 있어도 연결이 끊어지지 않도록 주기적으로 ping 전송
     */
    private void startHeartbeat(Long memberId, SseEmitter emitter) {
        scheduler.scheduleAtFixedRate(() -> {
            if (emitters.containsKey(memberId)) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("ping")
                            .data("heartbeat"));
                    log.debug("SSE 하트비트 전송 - memberId: {}", memberId);
                } catch (IOException e) {
                    log.warn("SSE 하트비트 전송 실패 - memberId: {}, 연결 종료", memberId);
                    emitters.remove(memberId);
                }
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.SECONDS);
    }
    
    /**
     * 특정 사용자에게 이벤트 전송
     * @param memberId 사용자 ID
     * @param eventName 이벤트 이름
     * @param data 전송할 데이터
     */
    public void sendToUser(Long memberId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
                log.info("SSE 이벤트 전송 성공 - memberId: {}, eventName: {}", memberId, eventName);
            } catch (IOException e) {
                log.error("SSE 이벤트 전송 실패 - memberId: {}, eventName: {}", memberId, eventName, e);
                emitters.remove(memberId);
            }
        } else {
            log.warn("SSE 연결이 없습니다 - memberId: {}", memberId);
        }
    }
    
    /**
     * 모든 연결된 사용자에게 브로드캐스트
     * @param eventName 이벤트 이름
     * @param data 전송할 데이터
     */
    public void broadcast(String eventName, Object data) {
        emitters.forEach((memberId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("SSE 브로드캐스트 실패 - memberId: {}", memberId, e);
                emitters.remove(memberId);
            }
        });
    }
    
    /**
     * 연결 종료
     * @param memberId 사용자 ID
     */
    public void disconnect(Long memberId) {
        SseEmitter emitter = emitters.remove(memberId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE 연결 종료 - memberId: {}", memberId);
        }
    }
    
    /**
     * 현재 연결된 사용자 수
     * @return 연결 수
     */
    public int getConnectedCount() {
        return emitters.size();
    }
}


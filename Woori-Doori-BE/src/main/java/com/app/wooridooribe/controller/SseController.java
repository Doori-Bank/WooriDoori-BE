package com.app.wooridooribe.controller;

import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.sse.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "SSE API", description = "Server-Sent Events 실시간 알림 API")
public class SseController {
    
    private final SseService sseService;
    
    /**
     * SSE 연결 엔드포인트
     * 클라이언트가 이 엔드포인트에 연결하면 실시간 이벤트를 받을 수 있습니다.
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "SSE 연결", description = "실시간 알림을 받기 위한 SSE 연결을 생성합니다")
    public ResponseEntity<SseEmitter> connect(Authentication authentication) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        
        log.info("SSE 연결 요청 - memberId: {}", memberId);
        
        SseEmitter emitter = sseService.createEmitter(memberId);
        
        return ResponseEntity.ok(emitter);
    }
    
    /**
     * SSE 연결 해제 엔드포인트
     */
    @PostMapping("/disconnect")
    @Operation(summary = "SSE 연결 해제", description = "SSE 연결을 종료합니다")
    public ResponseEntity<String> disconnect(Authentication authentication) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getMember().getId();
        
        sseService.disconnect(memberId);
        
        return ResponseEntity.ok("SSE 연결이 해제되었습니다");
    }
    
    /**
     * 테스트용 알림 전송 엔드포인트 (개발 환경용)
     * 실제 서버가 실행 중일 때 JUnit 테스트에서 사용할 수 있습니다.
     */
    @PostMapping("/test/send")
    @Operation(summary = "테스트용 알림 전송", description = "테스트를 위한 알림 전송 엔드포인트")
    public ResponseEntity<String> sendTestNotification(
            @RequestParam Long memberId,
            @RequestParam String eventName,
            @RequestBody(required = false) Object data) {
        
        log.info("테스트 알림 전송 요청 - memberId: {}, eventName: {}", memberId, eventName);
        
        if (data == null) {
            sseService.sendToUser(memberId, eventName, "테스트 알림");
        } else {
            sseService.sendToUser(memberId, eventName, data);
        }
        
        return ResponseEntity.ok("알림이 전송되었습니다");
    }
}


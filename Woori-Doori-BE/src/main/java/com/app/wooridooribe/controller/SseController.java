package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.SseSendRequestDto;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.repository.member.MemberRepository;
import com.app.wooridooribe.service.sse.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "SSE 알림", description = "Server-Sent Events를 통한 실시간 알림 API")
@RestController
@RequestMapping("/sse")
@RequiredArgsConstructor
@Slf4j
public class SseController {

    private final MemberRepository memberRepository;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    /**
     * SSE 연결
     */
    @Operation(summary = "SSE 연결", description = "Server-Sent Events를 통한 실시간 알림 연결을 설정합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보 없음")
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connect(
            @Parameter(hidden = true) Authentication authentication,
            HttpServletResponse response) {
        if (authentication == null) {
            log.warn("SSE 연결 실패: 인증 정보 없음");
            return null;
        }

        Long memberId = getMemberId(authentication);

        if (memberId == null) {
            log.warn("SSE 연결 실패: memberId 추출 실패");
            return null;
        }

        // SSE 응답 헤더 설정
        response.setHeader("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        response.setHeader("X-Accel-Buffering", "no"); // Nginx 버퍼링 방지

        // CORS는 SecurityConfig에서 중앙 관리하므로 여기서는 제거
        // 중복 설정 시 "multiple values" 오류 발생 가능

        SseEmitter emitter = sseService.createEmitter(memberId);

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("SSE 연결 성공"));
            log.info("SSE 연결 성공 - memberId: {}", memberId);
        } catch (IOException e) {
            log.error("SSE 초기 메시지 전송 실패 - memberId: {}", memberId, e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * 테스트용: 메시지 전송
     */
    @Operation(summary = "테스트 메시지 전송", description = "SSE를 통해 특정 사용자에게 테스트 메시지를 전송합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "회원을 찾을 수 없음 또는 SSE 연결이 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "메시지 전송 실패")
    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(
            @Parameter(description = "메시지 전송 요청 정보", required = true) @Valid @RequestBody SseSendRequestDto requestDto) {
        log.info("SSE 메시지 전송 요청: memberId={}, message={}", requestDto.getMemberId(), requestDto.getMessage());

        // 이메일로 회원 찾기
        Member member = memberRepository.findByMemberId(requestDto.getMemberId())
                .orElseThrow(() -> {
                    log.warn("SSE 메시지 전송 실패: 회원을 찾을 수 없음 - memberId={}", requestDto.getMemberId());
                    return new CustomException(ErrorCode.USER_NOT_FOUND);
                });

        Long memberId = member.getId();
        boolean sent = sseService.sendToUser(memberId, "message", requestDto.getMessage());

        if (sent) {
            log.info("SSE 메시지 전송 성공 - memberId: {}, message: {}", memberId, requestDto.getMessage());
            return ResponseEntity.ok("메시지 전송 성공");
        }

        log.warn("SSE 연결된 클라이언트 없음 - memberId: {}, 현재 연결 수: {}", memberId, sseService.getConnectedUserCount());
        return ResponseEntity.status(404).body("연결된 클라이언트가 없습니다. 현재 연결 수: " + sseService.getConnectedUserCount());
    }

    /**
     * 테스트용: GET으로 메시지 전송 (브라우저에서 쉽게 테스트)
     */
    @Operation(summary = "테스트 메시지 전송 (GET)", description = "GET 방식으로 SSE를 통해 테스트 메시지를 전송합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 메시지 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SSE 연결이 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "메시지 전송 실패")
    @GetMapping("/test")
    public ResponseEntity<String> testMessage(
            @Parameter(hidden = true) Authentication authentication) {
        Long memberId = getMemberId(authentication);

        if (memberId == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다");
        }

        String testMessage = "테스트 알림입니다! 현재 시간: " + java.time.LocalDateTime.now();
        boolean sent = sseService.sendToUser(memberId, "message", testMessage);

        if (sent) {
            log.info("SSE 테스트 메시지 전송 성공 - memberId: {}", memberId);
            return ResponseEntity.ok("테스트 메시지 전송 성공: " + testMessage);
        }

        return ResponseEntity.status(404).body("연결된 클라이언트가 없습니다. 먼저 /sse/connect에 연결하세요.");
    }

    /**
     * 테스트용: REPORT 알림 전송
     */
    @Operation(summary = "REPORT 타입 알림 전송", description = "리포트 알림을 전송합니다. 프론트엔드에서 '두리가 N월 소비 리포트를 가져왔습니다.' 메시지가 표시되고, /report로 이동합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "REPORT 알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SSE 연결이 없습니다. 먼저 /sse/connect에 연결하세요")
    @GetMapping("/test/report")
    public ResponseEntity<String> testReportNotification(
            @Parameter(hidden = true) Authentication authentication) {
        Long memberId = getMemberId(authentication);

        if (memberId == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다");
        }

        if (!sseService.isConnected(memberId)) {
            return ResponseEntity.status(404).body("SSE 연결이 없습니다. 먼저 /sse/connect에 연결하세요.");
        }

        int currentMonth = java.time.LocalDate.now().getMonthValue();
        sseService.sendReportNotification(memberId, currentMonth);
        return ResponseEntity.ok("REPORT 알림 전송 완료 (월: " + currentMonth + ")");
    }

    /**
     * 테스트용: DIARY 알림 전송
     */
    @Operation(summary = "DIARY 타입 알림 전송", description = "일기 알림을 전송합니다. 프론트엔드에서 일기 작성 알림이 표시되고, /calendar/diary/emotion?date=오늘날짜로 이동하여 바로 일기를 작성할 수 있습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "DIARY 알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SSE 연결이 없습니다. 먼저 /sse/connect에 연결하세요")
    @GetMapping("/test/diary")
    public ResponseEntity<String> testDiaryNotification(
            @Parameter(hidden = true) Authentication authentication) {
        Long memberId = getMemberId(authentication);

        if (memberId == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다");
        }

        if (!sseService.isConnected(memberId)) {
            return ResponseEntity.status(404).body("SSE 연결이 없습니다. 먼저 /sse/connect에 연결하세요.");
        }

        sseService.sendDiaryNotification(memberId);
        return ResponseEntity.ok("DIARY 알림 전송 완료");
    }

    /**
     * 테스트용: 커스텀 타입 알림 전송
     */
    @Operation(summary = "커스텀 타입 알림 전송", description = "지정한 타입으로 알림을 전송합니다. 타입: REPORT, diary, goal, achievement 등")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 정보가 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "SSE 연결이 없습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "알림 전송 실패")
    @PostMapping("/test/custom")
    public ResponseEntity<String> testCustomNotification(
            @Parameter(hidden = true) Authentication authentication,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "알림 데이터", required = true) @RequestBody Map<String, Object> notificationData) {
        Long memberId = getMemberId(authentication);

        if (memberId == null) {
            return ResponseEntity.status(401).body("인증 정보가 없습니다");
        }

        if (!sseService.isConnected(memberId)) {
            return ResponseEntity.status(404).body("SSE 연결이 없습니다. 먼저 /sse/connect에 연결하세요.");
        }

        try {
            String eventName = (String) notificationData.getOrDefault("type", "message");
            String jsonData = objectMapper.writeValueAsString(notificationData);
            sseService.sendToUser(memberId, eventName, jsonData);
            return ResponseEntity.ok("알림 전송 완료 (타입: " + eventName + ")");
        } catch (Exception e) {
            log.error("커스텀 알림 전송 실패 - memberId: {}", memberId, e);
            return ResponseEntity.status(500).body("알림 전송 실패: " + e.getMessage());
        }
    }

    public int getConnectedUserCount() {
        return sseService.getConnectedUserCount();
    }

    private Long getMemberId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        try {
            com.app.wooridooribe.jwt.MemberDetail principal = (com.app.wooridooribe.jwt.MemberDetail) authentication
                    .getPrincipal();
            return principal.getMember().getId();
        } catch (Exception e) {
            log.error("MemberId 추출 실패", e);
            return null;
        }
    }
}

package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ChatRequestDto;
import com.app.wooridooribe.controller.dto.ChatResponseDto;
import com.app.wooridooribe.entity.Member;
import com.app.wooridooribe.exception.CustomException;
import com.app.wooridooribe.exception.ErrorCode;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.chat.ChatBotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "채팅", description = "LLM 채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatBotService chatBotService;

    @Operation(summary = "채팅", description = "금융 소비 매니저 두리의 페르소나를 곁들인 LLM과 대화합니다. 사용자의 Goal과 소비 내역을 기반으로 답변합니다.")
    @PostMapping
    public ResponseEntity<ChatResponseDto> chat(
            @Valid @RequestBody ChatRequestDto request,
            Authentication authentication) {
        // 인증 정보 확인
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(ErrorCode.NO_TOKEN);
        }

        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Member member = principal.getMember();

        String response = chatBotService.chat(request.getMessage(), member);
        return ResponseEntity.ok(new ChatResponseDto(response));
    }
}

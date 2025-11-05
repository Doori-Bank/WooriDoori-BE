package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.MemberResponseDto;
import com.app.wooridooribe.service.member.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final MemberService memberService;

    /**
     * 전체 회원 조회 (관리자 전용)
     */
    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MemberResponseDto>>> getAllMembers() {
        log.info("관리자 - 전체 회원 조회");
        List<MemberResponseDto> members = memberService.getAllMembers();
        return ResponseEntity.ok(ApiResponse.res(200, "사용자들을 정보를 불러왔습니다!", members));
    }

    /**
     * 특정 회원 조회 (관리자 전용)
     */
    @GetMapping("/members/{memberId}")
    public ResponseEntity<ApiResponse<MemberResponseDto>> getMemberById(@PathVariable Long memberId) {
        log.info("관리자 - 회원 조회: {}", memberId);
        MemberResponseDto member = memberService.getMemberByIdForAdmin(memberId);
        return ResponseEntity.ok(ApiResponse.res(200, "사용자 정보를 불러왔습니다!", member));
    }
}

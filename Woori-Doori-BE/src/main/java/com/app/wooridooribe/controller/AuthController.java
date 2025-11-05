package com.app.wooridooribe.controller;


import com.app.wooridooribe.controller.dto.*;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.auth.AuthServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthServiceImpl authService;

    /**
     * 회원가입 (JoinDto 사용)
     */
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<LoginResponseDto>> join(@RequestBody JoinDto joinDto) {
        LoginResponseDto result = authService.join(joinDto);
        return ResponseEntity.ok(ApiResponse.res(200, "SUCCESS", result));
    }

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@RequestBody LoginDto loginDto) {
        LoginResponseDto result = authService.login(loginDto.getMemberId(), loginDto.getPassword());
        return ResponseEntity.ok(ApiResponse.res(200, "SUCCESS", result));
    }

    /**
     * 토큰 재발급
     */
    @PostMapping("/reissue")
    public ResponseEntity<TokenDto> reissue(@RequestBody TokenRequestDto tokenRequestDto) {
        return ResponseEntity.ok(authService.reissue(tokenRequestDto));
    }

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping("/user")
    public ResponseEntity<MemberDto> getUserDetails(Authentication authentication) {
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        
        MemberDto memberDto = MemberDto.builder()
                .memberId(principal.getUsername())
                .name(principal.getName())
                .phone(principal.getMember().getPhone())
                .build();
        
        return ResponseEntity.ok(memberDto);
    }
}
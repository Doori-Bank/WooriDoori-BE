package com.app.wooridooribe.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 테스트용 컨트롤러 - 비밀번호 암호화 확인용
 * TODO: 배포 전 삭제 필요!
 */
@Tag(name = "테스트", description = "개발용 테스트 API (배포 전 삭제 필요)")
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    
    private final PasswordEncoder passwordEncoder;
    
    @Operation(summary = "비밀번호 암호화", description = "평문 비밀번호를 BCrypt로 암호화합니다 (개발용)")
    @GetMapping("/encode")
    public String encodePassword(
            @Parameter(description = "암호화할 평문 비밀번호", required = true)
            @RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return "평문: " + password + "\n암호화: " + encoded + 
               "\n\n이 암호화된 값을 DB의 password 컬럼에 넣으세요!";
    }
    
    @Operation(summary = "비밀번호 매칭 테스트", description = "평문과 암호화된 비밀번호가 일치하는지 테스트합니다 (개발용)")
    @GetMapping("/match")
    public String testMatch(
            @Parameter(description = "평문 비밀번호", required = true)
            @RequestParam String raw,
            @Parameter(description = "암호화된 비밀번호", required = true)
            @RequestParam String encoded) {
        boolean matches = passwordEncoder.matches(raw, encoded);
        return "평문: " + raw + "\n암호화: " + encoded + "\n매칭 결과: " + matches;
    }
}


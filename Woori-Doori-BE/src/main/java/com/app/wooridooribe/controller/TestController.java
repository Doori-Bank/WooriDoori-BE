package com.app.wooridooribe.controller;

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
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 평문 비밀번호를 BCrypt로 암호화
     * 사용 예: GET http://localhost:8080/test/encode?password=test123
     */
    @GetMapping("/encode")
    public String encodePassword(@RequestParam String password) {
        String encoded = passwordEncoder.encode(password);
        return "평문: " + password + "\n암호화: " + encoded + 
               "\n\n이 암호화된 값을 DB의 password 컬럼에 넣으세요!";
    }
    
    /**
     * 비밀번호 매칭 테스트
     * 사용 예: GET http://localhost:8080/test/match?raw=test123&encoded=$2a$10$xxx...
     */
    @GetMapping("/match")
    public String testMatch(@RequestParam String raw, @RequestParam String encoded) {
        boolean matches = passwordEncoder.matches(raw, encoded);
        return "평문: " + raw + "\n암호화: " + encoded + "\n매칭 결과: " + matches;
    }
}


package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.MainDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.main.MainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "메인", description = "메인페이지 관련 API")
@RestController
@RequestMapping("/")
@RequiredArgsConstructor
@Slf4j
public class MainController {
    
    private final MainService mainService;
    
    @Operation(summary = "메인", description = "사용자의 소비 현황의 카테고리와 목표 금액, 현재 소비 현황을 보여줍니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "토큰값이 없거나 유효하지 않습니다")
    @GetMapping("main")
    public ResponseEntity<ApiResponse<MainDto>> main(Authentication authentication) {
        log.info("메인 페이지 조회 API 호출");
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        MainDto mainDto = mainService.getMainPageData(principal.getId());
        return ResponseEntity.ok(ApiResponse.res(200, "조회 완료", mainDto));
    }

}

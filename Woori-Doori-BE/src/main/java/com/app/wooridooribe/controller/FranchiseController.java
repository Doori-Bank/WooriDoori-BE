package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.FranchiseFileDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.fanchise.FranchiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "가맹점", description = "가맹점 관련 API")
@RestController
@RequestMapping("/franchise")
@RequiredArgsConstructor
@Slf4j
public class FranchiseController {

    private final FranchiseService franchiseService;


    @Operation(summary = "가맹점 TOP5 조회", description = "가맹점 TOP5 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{category}")
    public ResponseEntity<ApiResponse<List<FranchiseFileDto>>> search(
            @Parameter(hidden = true) Authentication authentication,
            @Parameter(description = "카테고리명", required = true) @PathVariable String category) {


        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        List<FranchiseFileDto> result = franchiseService.getFranchise(memberId, category);

        return ResponseEntity.ok(ApiResponse.res(200, "성공", result));
    }

}

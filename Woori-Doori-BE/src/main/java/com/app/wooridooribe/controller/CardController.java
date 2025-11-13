package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.CardCreateRequestDto;
import com.app.wooridooribe.controller.dto.CardDeleteRequestDto;
import com.app.wooridooribe.controller.dto.CardEditRequestDto;
import com.app.wooridooribe.controller.dto.CardResponseDto;
import com.app.wooridooribe.controller.dto.UserCardResponseDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.service.card.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "카드", description = "카드 목록 조회, 사용자 카드 등록, 수정 및 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/card")
public class CardController {

    private final CardService cardService;

    @Operation(summary = "카드 목록 조회", description = "DB에 있는 모든 카드 목록을 조회합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드 목록 조회 성공", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)", content = @Content(schema = @Schema(example = "{\"statusCode\": 401, \"errorResultMsg\": \"로그인을 해주세요\", \"errorName\": \"NO_TOKEN\"}")))
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponseDto>>> getCardList(
            Authentication authentication) {
        // DB에 있는 모든 카드 조회
        List<CardResponseDto> result = cardService.getAllCards();

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "카드 목록 조회 성공", result));
    }

    @Operation(summary = "사용자 카드 검증 및 불러오기", description = "기존 DB에 있는 사용자의 카드 정보를 검증해서 불러옵니다. card_alias가 제공되면 업데이트합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 카드 불러오기 완료", content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (예: 카드 비밀번호 불일치, 유효기간 불일치, 주민등록번호 불일치, CVC 불일치)", content = @Content(schema = @Schema(example = "{\"statusCode\": 400, \"errorResultMsg\": \"카드 비밀번호가 일치하지 않습니다.\", \"errorName\": \"INVALID_CARD_PASSWORD\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)", content = @Content(schema = @Schema(example = "{\"statusCode\": 401, \"errorResultMsg\": \"로그인을 해주세요\", \"errorName\": \"NO_TOKEN\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 또는 카드를 찾을 수 없음", content = @Content(schema = @Schema(example = "{\"statusCode\": 404, \"errorResultMsg\": \"해당 카드는 존재하지 않습니다.\", \"errorName\": \"CARD_ISNULL\"}")))
    @PatchMapping("/putCard")
    public ResponseEntity<ApiResponse<UserCardResponseDto>> createUserCard(
            Authentication authentication,
            @RequestBody(description = "카드 검증 요청 바디", required = true, content = @Content(schema = @Schema(implementation = CardCreateRequestDto.class))) @org.springframework.web.bind.annotation.RequestBody CardCreateRequestDto request) {
        // JWT에서 인증된 사용자 ID 가져오기
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        UserCardResponseDto result = cardService.createUserCard(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "사용자 카드 불러오기 완료", result));
    }

    @Operation(summary = "카드 삭제", description = "사용자 카드 ID(PK)로 카드를 삭제합니다. 실제 DB에서 삭제하지 않고 member_id만 NULL로 설정하여 화면에서만 삭제됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드 삭제 성공", content = @Content(schema = @Schema(example = "{\"statusCode\": 200, \"resultMsg\": \"카드 삭제 완료\", \"resultData\": null}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)", content = @Content(schema = @Schema(example = "{\"statusCode\": 401, \"errorResultMsg\": \"로그인을 해주세요\", \"errorName\": \"NO_TOKEN\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음", content = @Content(schema = @Schema(example = "{\"statusCode\": 404, \"errorResultMsg\": \"해당 카드는 존재하지 않습니다.\", \"errorName\": \"CARD_ISNULL\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인의 카드만 삭제 가능", content = @Content(schema = @Schema(example = "{\"statusCode\": 403, \"errorResultMsg\": \"해당 카드는 수정이 불가합니다.\", \"errorName\": \"CARD_ISNOTYOURS\"}")))
    @PatchMapping("/deleteCard")
    public ResponseEntity<ApiResponse<Void>> deleteCard(
            Authentication authentication,
            @RequestBody(description = "카드 삭제 요청 바디", required = true, content = @Content(schema = @Schema(implementation = CardDeleteRequestDto.class))) @org.springframework.web.bind.annotation.RequestBody CardDeleteRequestDto request) {
        // JWT에서 인증된 사용자 ID 가져오기
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        cardService.deleteCard(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "카드 삭제 완료", null));
    }

    @Operation(summary = "카드 별명 수정", description = "사용자 카드 ID(PK)로 카드 별명을 수정합니다. member_id가 등록되어 있는 카드만 수정 가능합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "카드 별명 수정 성공", content = @Content(schema = @Schema(example = "{\"statusCode\": 200, \"resultMsg\": \"카드 별명 수정 완료\", \"resultData\": null}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패 (JWT 필요)", content = @Content(schema = @Schema(example = "{\"statusCode\": 401, \"errorResultMsg\": \"로그인을 해주세요\", \"errorName\": \"NO_TOKEN\"}")))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드를 찾을 수 없음", content = @Content(schema = @Schema(example = "{\"statusCode\": 404, \"errorResultMsg\": \"해당 카드는 존재하지 않습니다.\", \"errorName\": \"CARD_ISNULL\"}")))
    @PatchMapping("/editCard")
    public ResponseEntity<ApiResponse<Void>> editCard(
            Authentication authentication,
            @RequestBody(description = "카드 별명 수정 요청 바디", required = true, content = @Content(schema = @Schema(implementation = CardEditRequestDto.class))) @org.springframework.web.bind.annotation.RequestBody CardEditRequestDto request) {
        // JWT에서 인증된 사용자 ID 가져오기
        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        cardService.editCardAlias(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "카드 별명 수정 완료", null));
    }
}

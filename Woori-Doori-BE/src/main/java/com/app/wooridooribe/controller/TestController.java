package com.app.wooridooribe.controller;

import com.app.wooridooribe.controller.dto.ApiResponse;
import com.app.wooridooribe.controller.dto.CardCreateRequestDto;
import com.app.wooridooribe.controller.dto.TestCardInfoDto;
import com.app.wooridooribe.controller.dto.UserCardResponseDto;
import com.app.wooridooribe.jwt.MemberDetail;
import com.app.wooridooribe.repository.memberCard.MemberCardRepository;
import com.app.wooridooribe.service.card.CardService;
import com.app.wooridooribe.service.goal.GoalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
@Slf4j
public class TestController {
    
    private final PasswordEncoder passwordEncoder;
    private final GoalService goalService;
    private final CardService cardService;
    private final MemberCardRepository memberCardRepository;
    
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

    @Operation(summary = "목표 점수 수동 계산", description = "월말 스케줄러 대신 수동으로 배치 점수 계산을 실행합니다 (개발용)")
    @GetMapping("/goal-score/calculate")
    public String calculateGoalScoresManually() {
        try {
            int processedCount = goalService.calculateAllActiveUsersScores();
            return "배치 점수 계산 완료 - 처리된 유저 수: " + processedCount;
        } catch (Exception e) {
            log.error("수동 배치 점수 계산 실패", e);
            return "배치 점수 계산 실패: " + e.getMessage();
        }
    }

    @Operation(summary = "카드 등록 (테스트용 CVC 검증 생략)", description = "기존 카드 정보를 CVC 검증 없이 연결합니다. 개발/테스트 전용 엔드포인트입니다.")
    @PatchMapping("/card/putCard/no-cvc")
    public ResponseEntity<ApiResponse<UserCardResponseDto>> createUserCardWithoutCvc(
            @Parameter(hidden = true) Authentication authentication,
            @RequestBody CardCreateRequestDto request) {

        MemberDetail principal = (MemberDetail) authentication.getPrincipal();
        Long memberId = principal.getId();

        UserCardResponseDto result = cardService.createUserCardWithoutCvc(memberId, request);

        return ResponseEntity.ok(
                ApiResponse.res(HttpStatus.OK.value(), "[TEST] CVC 없이 카드 등록 완료", result)
        );
    }

    @Operation(summary = "카드 원본 정보 조회 (테스트용)", description = "회원 이름 + 주민번호 정보로 카드 원본 데이터를 조회합니다.")
    @GetMapping("/card-info")
    public ResponseEntity<ApiResponse<TestCardInfoDto>> getCardInfo(
            @Parameter(description = "카드 사용자 이름", required = true) @RequestParam String memberName,
            @Parameter(description = "주민등록번호 앞자리(6자리)", required = true) @RequestParam String registNum,
            @Parameter(description = "주민등록번호 뒷자리(1자리 또는 7자리)", required = true) @RequestParam String registBack
    ) {

        return memberCardRepository
                .findFirstByCardUserNameAndCardUserRegistNumAndCardUserRegistBack(memberName, registNum, registBack)
                .map(memberCard -> ResponseEntity.ok(
                        ApiResponse.res(HttpStatus.OK.value(), "[TEST] 카드 정보 조회 성공", TestCardInfoDto.from(memberCard))
                ))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.res(HttpStatus.NOT_FOUND.value(), "[TEST] 카드 정보를 찾을 수 없습니다.", null)));
    }
}

